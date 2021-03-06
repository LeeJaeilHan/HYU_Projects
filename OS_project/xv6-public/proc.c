#include "types.h"
#include "defs.h"
#include "param.h"
#include "memlayout.h"
#include "mmu.h"
#include "x86.h"
#include "proc.h"
#include "spinlock.h"

struct {
  struct spinlock lock;
  struct proc proc[NPROC];
} ptable;

static struct proc *initproc;

int nextpid = 1;

extern void forkret(void);
extern void trapret(void);

static void wakeup1(void *chan);

void
pinit(void)
{
  initlock(&ptable.lock, "ptable");
}


//PAGEBREAK: 32
// Look in the process table for an UNUSED proc.
// If found, change state to EMBRYO and initialize
// state required to run in the kernel.
// Otherwise return 0.
static struct proc*
allocproc(void)
{
  struct proc *p;
  char *sp;

  acquire(&ptable.lock);

  for(p = ptable.proc; p < &ptable.proc[NPROC]; p++)
    if(p->state == UNUSED)
      goto found;

  release(&ptable.lock);
  return 0;

found:
  p->state = EMBRYO;
  p->pid = nextpid++;

  release(&ptable.lock);

  // Allocate kernel stack.
  if((p->kstack = kalloc()) == 0){
    p->state = UNUSED;
    return 0;
  }
  sp = p->kstack + KSTACKSIZE;

  // Leave room for trap frame.
  sp -= sizeof *p->tf;
  p->tf = (struct trapframe*)sp;

  // Set up new context to start executing at forkret,
  // which returns to trapret.
  sp -= 4;
  *(uint*)sp = (uint)trapret;

  sp -= sizeof *p->context;
  p->context = (struct context*)sp;
  memset(p->context, 0, sizeof *p->context);
  p->context->eip = (uint)forkret;

  /*int i;
  for(i = 0; i < NPROC; i++)
  {
      p->arr_ret[i] = 0;
      p->t[i] = 0;
  }

  p->tid = 0;
  p->isThread = 0;*/

  return p;
}

//PAGEBREAK: 32
// Set up first user process.
void
userinit(void)
{
  struct proc *p;
  extern char _binary_initcode_start[], _binary_initcode_size[];

  p = allocproc();
  acquire(&ptable.lock);
  initproc = p;
  if((p->pgdir = setupkvm()) == 0)
    panic("userinit: out of memory?");
  inituvm(p->pgdir, _binary_initcode_start, (int)_binary_initcode_size);
  p->sz = PGSIZE;
  memset(p->tf, 0, sizeof(*p->tf));
  p->tf->cs = (SEG_UCODE << 3) | DPL_USER;
  p->tf->ds = (SEG_UDATA << 3) | DPL_USER;
  p->tf->es = p->tf->ds;
  p->tf->ss = p->tf->ds;
  p->tf->eflags = FL_IF;
  p->tf->esp = PGSIZE;
  p->tf->eip = 0;  // beginning of initcode.S

  safestrcpy(p->name, "initcode", sizeof(p->name));
  p->cwd = namei("/");

  // this assignment to p->state lets other cores
  // run this process. the acquire forces the above
  // writes to be visible, and the lock is also needed
  // because the assignment might not be atomic.

  p->state = RUNNABLE;

  release(&ptable.lock);
}

// Grow current process's memory by n bytes.
// Return 0 on success, -1 on failure.
int
growproc(int n)
{
  uint sz;

  sz = proc->sz;
  if(n > 0){
    if((sz = allocuvm(proc->pgdir, sz, sz + n)) == 0)
      return -1;
  } else if(n < 0){
    if((sz = deallocuvm(proc->pgdir, sz, sz + n)) == 0)
      return -1;
  }
  proc->sz = sz;
  switchuvm(proc);
  return 0;
}

//set_cpu_share
int
set_cpu_share(int cpu_share)
{
    return 0;
}

//thread create
int
thread_create(thread_t* thread, void* (*start_routine)(void*), void* arg)
{
    /////////////////////////
    ///////////////////
  int i;
  char* sp;
  uint sz;
  struct proc *np;
  // Allocate process.
  // allocate new kstack
  if((np = allocproc()) == 0){
    return -1;
  }

  //thread shares its page directory
  np->pgdir = proc->pgdir;

  //main thread is pointed by parent member variable
  np->parent = proc;

  //copy current cpu, register's state to new kstack
  *np->tf = *proc->tf;  
  np->tf->eax = 0;

  //make start point start_routine
  np->tf->eip = (uint)start_routine;

  //represent if this proc struct is used for thread
  np->isThread = 1;

  //main thread check created thread by t[i] array
  for(i=0; i<64; i++)
  {
      if(proc->t[i] == 0)
      {
          proc->t[i] = 1;
          np->tid = i;
          *thread = i;
          break;
      }
  }

  //check location which new stack will be created in 
  uint ba = proc->sz + np->tid * PGSIZE;
  np->ustack = ba + PGSIZE;

  //allocate new user stack
  if((sz = allocuvm(np->pgdir, ba, ba + PGSIZE)) == 0)
  {
      return -1;
  }

  np->sz = sz;
  sp = (char*)sz;

  //push arg and fake address
  //as thread will regard new stack as running main func
  //arg is start_routine function's argument
  sp -= 4;
  *(uint*)sp = (uint)arg;
  sp -= 4;
  *(uint*)sp = 0xffffffff;

  //point new stack
  np->tf->esp = (uint)sp;

  for(i = 0; i < NOFILE; i++)
    if(proc->ofile[i])
      np->ofile[i] = filedup(proc->ofile[i]);
  np->cwd = idup(proc->cwd);

  safestrcpy(np->name, proc->name, sizeof(proc->name));


  acquire(&ptable.lock);
  np->state = RUNNABLE;
  release(&ptable.lock);

  return 0;
}

//thread exit
void
thread_exit(void *retval)
{
  int fd;

  if(proc == initproc)
    panic("init exiting");

  // Close all open files.
  for(fd = 0; fd < NOFILE; fd++){
    if(proc->ofile[fd]){
      fileclose(proc->ofile[fd]);
      proc->ofile[fd] = 0;
    }
  }

  begin_op();
  iput(proc->cwd);
  end_op();
  proc->cwd = 0;

  acquire(&ptable.lock);

  // Parent might be sleeping in wait().
  // wakeup parent so that parent(main thread) will free 
  // current thread's stack and kstack
  wakeup1(proc->parent);
      
  //deliver retval argument
  proc->parent->arr_ret[proc->tid] = retval;

  // Jump into the scheduler, never to return.
  //cprintf("tid : %d,  exit retval : %d\n",proc->tid, (int)proc->parent->arr_ret[proc->tid]);
  proc->state = ZOMBIE;
  sched();
  panic("zombie exit");
}

//thread join
int
thread_join(thread_t thread, void **retval)
{
  struct proc *p;
  int havekids;

  acquire(&ptable.lock);
  for(;;){
    // Scan through table looking for exited children.
    havekids = 0;
    for(p = ptable.proc; p < &ptable.proc[NPROC]; p++){
        //check if p's main thread is proc
        //check if p's tid is thread
      if(p->parent != proc && p->isThread != 1)
        continue;
      havekids = 1;
      //check if p is exited
      //check if p is thread
      if(p->state == ZOMBIE && p->tid == thread){
        // push arr_ret into retval
        *retval = proc->arr_ret[p->tid];
        //cprintf("tid : %d, join retval : %d\n", p->tid, (int)proc->arr_ret[p->tid]);
        //dealloc user stack which the thread used
        deallocuvm(p->pgdir, p->ustack, p->ustack - PGSIZE);

        //dealloc kernel syack which the thread used
        kfree(p->kstack);
        p->kstack = 0;
        p->pid = 0;
        p->parent = 0;
        p->name[0] = 0;
        p->killed = 0;
        p->state = UNUSED;

        //initialize member variables related to thread
        p->isThread = 0;
        proc->t[thread] = 0;
        p->tid = 0;

        release(&ptable.lock);
        return 0;
      }
    }

    // No point waiting if we don't have any children.
    if(!havekids || proc->killed){
      release(&ptable.lock);
      return -1;
    }

    // Wait for children to exit.  (See wakeup1 call in proc_exit.)
    sleep(proc, &ptable.lock);  //DOC: wait-sleep
  }
    //////////////////
    /////////////////
}

// Create a new process copying p as the parent.
// Sets up stack to return as if from system call.
// Caller must set state of returned proc to RUNNABLE.
int
fork(void)
{
  int i, pid;
  struct proc *np;

  // Allocate process.
  if((np = allocproc()) == 0){
    return -1;
  }

  // Copy process state from p.
  if((np->pgdir = copyuvm(proc->pgdir, proc->sz)) == 0){
    kfree(np->kstack);
    np->kstack = 0;
    np->state = UNUSED;
    return -1;
  }
  np->sz = proc->sz;
  np->parent = proc;
  *np->tf = *proc->tf;

  // Clear %eax so that fork returns 0 in the child.
  np->tf->eax = 0;

  for(i = 0; i < NOFILE; i++)
    if(proc->ofile[i])
      np->ofile[i] = filedup(proc->ofile[i]);
  np->cwd = idup(proc->cwd);

  safestrcpy(np->name, proc->name, sizeof(proc->name));

  pid = np->pid;

  acquire(&ptable.lock);

  np->state = RUNNABLE;

  release(&ptable.lock);

  return pid;
}

// Exit the current process.  Does not return.
// An exited process remains in the zombie state
// until its parent calls wait() to find out it exited.
void
exit(void)
{
  struct proc *p;
  int fd;
  ///////
  //////
  //if thread call the exit system call
  if(proc->isThread == 1)
  {
      acquire(&ptable.lock);

      for(p = ptable.proc; p < &ptable.proc[NPROC]; p++)
      {
          //find procs which are thread and have same parent and not proc
          if(p->isThread == 1 && proc->parent == p->parent && proc != p)
          {
              release(&ptable.lock);
              // Close all open files.
              for(fd = 0; fd < NOFILE; fd++){
                if(p->ofile[fd]){
                  fileclose(p->ofile[fd]);
                  p->ofile[fd] = 0;
                }
              }

              begin_op();
              iput(p->cwd);
              end_op();
              p->cwd = 0;

              acquire(&ptable.lock);

              //dealloc thread p's kstack
              kfree(p->kstack);
              p->kstack = 0;
              p->pid = 0;
              p->parent = 0;
              p->name[0] = 0;
              p->killed = 0;

              //dealloc thread p's user stack
              deallocuvm(p->pgdir, p->ustack, p->ustack - PGSIZE);
              p->state = UNUSED;
              //
              //if2 end
          }
          //for end
      }

      //make parent zombie
      for(p = ptable.proc; p <&ptable.proc[NPROC]; p++)
      {
          //find proc's parent
          if(p == proc->parent)
          {
              release(&ptable.lock);
              // Close all open files.
              for(fd = 0; fd < NOFILE; fd++){
                if(p->ofile[fd]){
                  fileclose(p->ofile[fd]);
                  p->ofile[fd] = 0;
                }
              }

              begin_op();
              iput(p->cwd);
              end_op();
              p->cwd = 0;

              p->state = ZOMBIE;
              acquire(&ptable.lock);
              //if end
          }
          //for end
      }

      release(&ptable.lock);
      // Close all open files.
      for(fd = 0; fd < NOFILE; fd++){
        if(proc->ofile[fd]){
          fileclose(proc->ofile[fd]);
          proc->ofile[fd] = 0;
        }
      }

      begin_op();
      iput(proc->cwd);
      end_op();
      proc->cwd = 0;

      acquire(&ptable.lock);

      proc->pid = 0;
      proc->name[0] = 0;
      proc->killed = 0;

      deallocuvm(proc->pgdir, proc->ustack, proc->ustack - PGSIZE);

      proc->state = UNUSED;

      wakeup1(proc->parent->parent);
      proc->parent = 0;
      //if end
  }

  ///////
  /////
  //the normal case that process call the exit system call
  //in this case, wakeup process's parent
  else
  {
      //this first segment is same as original exit
      /////////////////////////////////
      if(proc == initproc)
        panic("init exiting");

      // Close all open files.
      for(fd = 0; fd < NOFILE; fd++){
        if(proc->ofile[fd]){
          fileclose(proc->ofile[fd]);
          proc->ofile[fd] = 0;
        }
      }

      begin_op();
      iput(proc->cwd);
      end_op();
      proc->cwd = 0;

      acquire(&ptable.lock);

      // Parent might be sleeping in wait().
      wakeup1(proc->parent);

      // Pass abandoned children to init.
      for(p = ptable.proc; p < &ptable.proc[NPROC]; p++)
      {
          if(p->parent == proc)
          {
              p->parent = initproc;
              if(p->state == ZOMBIE)
                wakeup1(initproc);

              //if p is thread, deallocate thread p's kstack and user stack
              if(p->isThread == 1)
              {
                  release(&ptable.lock);

                  for(fd = 0; fd < NOFILE; fd++){
                    if(p->ofile[fd]){
                      fileclose(p->ofile[fd]);
                      p->ofile[fd] = 0;
                    }
                  }
                  begin_op();
                  iput(p->cwd);
                  end_op();
                  p->cwd = 0;

                  acquire(&ptable.lock);

                  kfree(p->kstack);
                  p->kstack = 0;
                  p->pid = 0;
                  p->parent = 0;
                  p->name[0] = 0;
                  p->killed = 0;

                  deallocuvm(p->pgdir, p->ustack, p->ustack - PGSIZE);

                  p->state = UNUSED;
                  //if2 end
              }
          //if 1 end
          }
      //for end
      }

      // Jump into the scheduler, never to return.
      proc->state = ZOMBIE;

  //else end
  }
  sched();
  panic("zombie exit");
}

// Wait for a child process to exit and return its pid.
// Return -1 if this process has no children.
int
wait(void)
{
  struct proc *p;
  int havekids, pid;

  acquire(&ptable.lock);
  for(;;){
    // Scan through table looking for exited children.
    havekids = 0;
    for(p = ptable.proc; p < &ptable.proc[NPROC]; p++){
      if(p->parent != proc)
        continue;
      havekids = 1;
      if(p->state == ZOMBIE && p->isThread == 0){
        // Found one.
        pid = p->pid;
        kfree(p->kstack);
        p->kstack = 0;
        freevm(p->pgdir);
        p->pid = 0;
        p->parent = 0;
        p->name[0] = 0;
        p->killed = 0;
        p->state = UNUSED;
        release(&ptable.lock);
        return pid;
      }
    }

    // No point waiting if we don't have any children.
    if(!havekids || proc->killed){
      release(&ptable.lock);
      return -1;
    }

    // Wait for children to exit.  (See wakeup1 call in proc_exit.)
    sleep(proc, &ptable.lock);  //DOC: wait-sleep
  }
}



//PAGEBREAK: 42
// Per-CPU process scheduler.
// Each CPU calls scheduler() after setting itself up.
// Scheduler never returns.  It loops, doing:
//  - choose a process to run
//  - swtch to start running that process
//  - eventually that process transfers control
//      via swtch back to the scheduler.
void
scheduler(void)
{
    struct proc* p;
    for(;;){
        // Enable interrupts on this processor.
        sti();

        // Loop over process table looking for process to run.
        acquire(&ptable.lock);
        for(p = ptable.proc; p < &ptable.proc[NPROC]; p++)
        {
           if(p->state != RUNNABLE)
               continue;   
           //start process
            proc = p;
            switchuvm(p);
            p->state = RUNNING;
            swtch(&cpu->scheduler, p->context);
            switchkvm();
           //finish process
            proc = 0;
        }
        release(&ptable.lock);
    }
}

// Enter scheduler.  Must hold only ptable.lock
// and have changed proc->state. Saves and restores
// intena because intena is a property of this
// kernel thread, not this CPU. It should
// be proc->intena and proc->ncli, but that would
// break in the few places where a lock is held but
// there's no process.
void
sched(void)
{
  int intena;

  if(!holding(&ptable.lock))
    panic("sched ptable.lock");
  if(cpu->ncli != 1)
    panic("sched locks");
  if(proc->state == RUNNING)
    panic("sched running");
  if(readeflags()&FL_IF)
    panic("sched interruptible");
  intena = cpu->intena;
  swtch(&proc->context, cpu->scheduler);
  cpu->intena = intena;
}

// Give up the CPU for one scheduling round.
void
yield(void)
{
  acquire(&ptable.lock);  //DOC: yieldlock
  proc->state = RUNNABLE;
  sched();
  release(&ptable.lock);
}

// A fork child's very first scheduling by scheduler()
// will swtch here.  "Return" to user space.
void
forkret(void)
{
  static int first = 1;
  // Still holding ptable.lock from scheduler.
  release(&ptable.lock);

  if (first) {
    // Some initialization functions must be run in the context
    // of a regular process (e.g., they call sleep), and thus cannot
    // be run from main().
    first = 0;
    iinit(ROOTDEV);
    initlog(ROOTDEV);
  }

  // Return to "caller", actually trapret (see allocproc).
}

// Atomically release lock and sleep on chan.
// Reacquires lock when awakened.
void
sleep(void *chan, struct spinlock *lk)
{
  if(proc == 0)
    panic("sleep");

  if(lk == 0)
    panic("sleep without lk");

  // Must acquire ptable.lock in order to
  // change p->state and then call sched.
  // Once we hold ptable.lock, we can be
  // guaranteed that we won't miss any wakeup
  // (wakeup runs with ptable.lock locked),
  // so it's okay to release lk.
  if(lk != &ptable.lock){  //DOC: sleeplock0
    acquire(&ptable.lock);  //DOC: sleeplock1
    release(lk);
  }

  // Go to sleep.
  proc->chan = chan;
  proc->state = SLEEPING;
  sched();

  // Tidy up.
  proc->chan = 0;

  // Reacquire original lock.
  if(lk != &ptable.lock){  //DOC: sleeplock2
    release(&ptable.lock);
    acquire(lk);
  }
}

//PAGEBREAK!
// Wake up all processes sleeping on chan.
// The ptable lock must be held.
static void
wakeup1(void *chan)
{
  struct proc *p;

  for(p = ptable.proc; p < &ptable.proc[NPROC]; p++)
    if(p->state == SLEEPING && p->chan == chan)
      p->state = RUNNABLE;
}

// Wake up all processes sleeping on chan.
void
wakeup(void *chan)
{
  acquire(&ptable.lock);
  wakeup1(chan);
  release(&ptable.lock);
}

// Kill the process with the given pid.
// Process won't exit until it returns
// to user space (see trap in trap.c).
int
kill(int pid)
{
  struct proc *p;

  acquire(&ptable.lock);
  for(p = ptable.proc; p < &ptable.proc[NPROC]; p++){
    if(p->pid == pid){
      p->killed = 1;
      // Wake process from sleep if necessary.
      if(p->state == SLEEPING)
        p->state = RUNNABLE;
      release(&ptable.lock);
      return 0;
    }
  }
  release(&ptable.lock);
  return -1;
}

//PAGEBREAK: 36
// Print a process listing to console.  For debugging.
// Runs when user types ^P on console.
// No lock to avoid wedging a stuck machine further.
void
procdump(void)
{
  static char *states[] = {
  [UNUSED]    "unused",
  [EMBRYO]    "embryo",
  [SLEEPING]  "sleep ",
  [RUNNABLE]  "runble",
  [RUNNING]   "run   ",
  [ZOMBIE]    "zombie"
  };
  int i;
  struct proc *p;
  char *state;
  uint pc[10];

  for(p = ptable.proc; p < &ptable.proc[NPROC]; p++){
    if(p->state == UNUSED)
      continue;
    if(p->state >= 0 && p->state < NELEM(states) && states[p->state])
      state = states[p->state];
    else
      state = "???";
    cprintf("%d %s %s", p->pid, state, p->name);
    if(p->state == SLEEPING){
      getcallerpcs((uint*)p->context->ebp+2, pc);
      for(i=0; i<10 && pc[i] != 0; i++)
        cprintf(" %p", pc[i]);
    }
    cprintf("\n");
  }
}
