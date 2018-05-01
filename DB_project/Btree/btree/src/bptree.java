import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


class PairLeaf {
	private int l;
	private int r;
	public PairLeaf(int l, int r){
		this.l = l;
		this.r = r;
	}
	
	public int getL(){ return l;}
	public int getR(){ return r;}
	public void setL(int l){this.l = l;}
	public void setR(int r){this.r = r;}
}

class PairIndex {
	private int l;
	private Node r;
	public PairIndex(int l, Node r){
		this.l = l;
		this.r = r;
	}
	
	public int getL(){ return l;}
	public Node getR(){ return r;}
	public void setL(int l){this.l = l;}
	public void setR(Node r){this.r = r;}
}


abstract class Node
{
	public int maxsize;
	public Node r;
	public IndexNode parent;
	public boolean isLeaf;
	
	public Node(int degree)
	{
		maxsize = degree;
		r = null;
		parent = null;
	}
	public int gethalf()
	{
		if(maxsize % 2 == 0)
			return maxsize/2;
		else
			return (maxsize+1)/2;
	}
	public abstract void delete(int place, Bplustree tree);
	public abstract int searchForInsert(int key);
	public abstract int searchKey(int key);
	public abstract void printSearch(int key);
	public abstract Node searchLeaf(int key);
	public abstract void traverse();	
}

class LeafNode extends Node
{
	public List<PairLeaf> p;
	public LeafNode l;
	
	public LeafNode(int degree)
	{
		super(degree);
		l = null;
		p = new ArrayList<PairLeaf>();
		super.isLeaf = true;
	}
	public void traverse()
	{
		System.out.print("[");
		for(int i=0; i<p.size(); i++)
		{
			System.out.print("(" + p.get(i).getL()+", "+p.get(i).getR()+")");
		}
		System.out.println("]");
	}
	public int searchKey(int key)
	{
		for(int i=0; i<this.p.size(); i++)
		{
			if(this.p.get(i).getL() == key)
			{
				return i;
			}
		}
		//���� key �� ��ġ�ϴ� key �� ���ٸ� -1 ��ȯ
		return -1;
		//searchKey end
	}
	public int searchForInsert(int key)
	{
		if(this.p.size() == 0)
			return 0;
		for(int i=0; i<this.p.size(); i++)
		{
			//������ key���� ū key���� ���ʷ� �߰��� ��ġ ��ȯ
			if(this.p.get(i).getL() > key)
			{
				return i;
			}
		}
		//������ key���� ū key�� ���� ��� ���� �� ���� �ڿ� ����
		return this.p.size();
	}
	public Node searchLeaf(int key)
	{
		return this;
	}
	public void printSearch(int key)
	{
		for(int i=0; i<this.p.size(); i++)
		{
			if(this.p.get(i).getL() == key)
			{
				System.out.println(p.get(i).getR());
				return;
			}
			//for end
		}
		System.out.println("NOT FOUND");
		//printSearch end
	}
	public void insert(int key, int value, Bplustree tree)
	{
		//��尡 �� ���ִ� ���
		if(p.size() == maxsize)
		{
			split(key, value, tree);
		}
		//��尡 �� ������ ���� ���
		else
		{
			//�� ���� pair ����
			PairLeaf tmp = new PairLeaf(key, value);
			//���� pair �� ���� ��� list �� ����
			//list �� ���� �� ������ �����ǵ��� ���� ��ġ ����
			int place = searchForInsert(key);
			p.add(place, tmp);
		}
	}
	public void delete(int key, Bplustree tree)
	{
		int half = maxsize/2; //Ȧ���� ��� ���ݺ��� �۴�.
		int dkey = this.searchKey(key);
		//delete ���ϴ� key ���� ��ġ�ϴ� key���� ���ٸ�
		if(dkey == -1)
		{
			System.out.println("NOT FOUND");
			return;
		}
		//delete ���ϴ� key���� ��ġ�ϴ� key���� �ִٸ�
		else
		{
			//leaf��尡 root�� case
			if(this.parent == null)
			{
				this.p.remove(dkey);
				return;
			}
			
			//������ ������ half�� case
			if(this.p.size() == half)
			{
				//�켱 ����
				this.p.remove(dkey);
				
				//���� ���� ��� �ִ� case
				if(this.l != null && this.l.parent==this.parent)
				{
					//���� borrow
					if(this.l.p.size() > half)
					{
						//left sibling�� ������ ���Ҹ� �����´�
						PairLeaf tmp = this.l.p.remove((this.l.p.size()-1));
						this.p.add(0, tmp);
						int place = this.parent.searchForInsert(tmp.getL());
						this.parent.p.get(place).setL(tmp.getL());
						return;
					}
					//���� ��尡 half�� case
					//������ ���� ���� �����ϴ� case
					else if(this.r!=null && this.r.parent == this.parent)
					{
						LeafNode right = (LeafNode) this.r;
						//������ borrow
						if(right.p.size() > half)
						{
							PairLeaf tmp = right.p.remove(0);
							this.p.add(tmp);
							int place = this.parent.searchForInsert(tmp.getL()) - 1;
							this.parent.p.get(place).setL(right.p.get(0).getL());
							return;
						}
						//���ʳ��� merge, 
						//right sibling �ִ� case 
						else
						{
							LeafNode left = this.l;
							//���� ����� �޼ҵ��� ���� ���� ��� �̵�
							for(int i =0; i<half-1; i++)
							{
								PairLeaf tmp = this.p.remove(0);
								left.p.add(tmp);
							}
							
							//������ ������带 �����ϰ� ������ ���Ḯ��Ʈ �籸��
							left.r = right;
							right.l = left;
							
							//�θ� ��忡 ����� left node�� ��ġ�� �����Ų��.
							//�θ��忡�� ������ pair�� ��ġ = place
							int place = left.parent.searchForInsert(left.p.get(0).getL());
							left.parent.p.get(place+1).setR(left);
							
							//�θ��忡�� place��ġ�� pair�� �����Ѵ�
							this.parent.delete(place, tree);
							return;
						}
					}
					//���ʳ��� merge
					//�ε�������� ������ ��������� case(������ ���� ����)
					else
					{
						LeafNode left = this.l;
						//���� ����� �޼ҵ��� ���� ���� ��� �̵�
						for(int i = 0; i < half-1; i++)
						{
							PairLeaf tmp = this.p.remove(0);
							left.p.add(tmp);
						}
						
						//���� ��忡 �ش��ϴ� �θ� ����� Ű���� ��������ش�.
						//�θ��忡�� ������ pair�� ��ġ�� ã�´�
						int place = left.parent.searchForInsert(left.p.get(0).getL());
						
						//���� ��� ���� �� ���Ḯ��Ʈ �籸��
						left.l.r = this;
						this.l = left.l;
						
						//�θ��忡�� place��ġ�� pair�� �����Ѵ�
						left.parent.r = left;
						this.parent.delete(place, tree);
						return;
					}
				}
				//���� ������尡 ���� ������ ������尡 �ִ� case
				//���� ���� ����� ���.
				else if(this.r!=null && this.r.parent == this.parent)
				{
					LeafNode right = (LeafNode) this.r;
					//������ borrow
					if(right.p.size() > half)
					{
						PairLeaf tmp = right.p.remove(0);
						this.p.add(tmp);
						int place = this.parent.searchForInsert(right.p.get(0).getL()) - 1;
						this.parent.p.get(place).setL(right.p.get(0).getL());
						return;
					}
					//�����ʳ��� merge
					else
					{
						//������ ����� ���ҵ��� ��� ������ ���� �̵�
						for(int i =half-2; i>=0; i--)
						{
							PairLeaf tmp = this.p.remove(i);
							right.p.add(0,tmp);
						}
						//�θ��忡�� ������ pair�� ��ġ ��´�
						int place = this.parent.searchForInsert(right.p.get(0).getL());
						
						//right����� ���� ������ �������
						right.l = null;
						
						//�θ��忡�� place��ġ�� pair ����
						this.parent.delete(place, tree);
						return;
					}
				}
				//���� ������ ���� ��� ��� ����
				//root ��� case
				else
				{
					this.p.remove(dkey);
					return;
				}
				//���Ұ����� ������ ��� end
			}
			//���Ұ� ���ݺ��� ���ٸ�
			else if(this.p.size() > half)
			{
				this.p.remove(dkey);
				return;
			}
			//���Ұ� ���ݺ��� ���ٸ� root ��� case
			else
			{
				this.p.remove(dkey);
				return;
			}
		}
		//delete end
	}
	public void split(int key, int value, Bplustree tree)
	{
		//PairLeaf ���� ����
		PairLeaf tmp = new PairLeaf(key, value);
		
		//insert�� pair�� list(p)�� ���� �� �����Ѵ�
		//���� �ش� ����� ���Ҵ� maxsize���� 1�� �� ����.
		int place = searchForInsert(key);
		p.add(place, tmp);
		
		//leaf node ����
		LeafNode leaf = new LeafNode(this.maxsize);
		
		//�� leaf node �� �տ������� ���ݱ����� ���Ҹ� �̵���Ų��
		//���� ����� ���Ұ� 1�� �� ���ų� ����
		int half = gethalf();
		
		//���� ���� �̵���Ų��
		for(int i=0; i<half; i++)
		{
			//ù��° ���Ҹ� ��� �̵���Ų��(half Ƚ����ŭ)
			PairLeaf pairleaf = p.remove(0);
			leaf.p.add(pairleaf);
		}
		//���� ���� leaf ��� right ����
		leaf.r = this;
		//���� ��尡 �����ϴ� ���
		if(this.l != null)
		{
			//leaf �� ���� ����� ���� ���� right ����
			this.l.r = leaf;
			//leaf �� ���� ����� ���� ���� left ����
			leaf.l = this.l;
		}
		//���� ���� leaf ��� left ����
		this.l = leaf;
		//�θ� ���� �ø� key �� �̴�.
		int upperkey = this.p.get(0).getL();
		//�θ� ��尡 �������� �ʴ´ٸ� ���� �����Ѵ�
		//�� ��� �θ���� root �� �ȴ�
		if(this.parent == null)
		{
			IndexNode index =new IndexNode(maxsize);
			//�θ��忡 ���� ����� ���� ���� key �� ���ο� ���(pair)�� �����Ѵ�.
			index.insert(upperkey, leaf, tree);
			//�θ����� rightmost child �� ���� ���� ����
			index.r = this;
			//���� ���� ���ο� ����� �θ� index �� ����
			this.parent = index;
			leaf.parent = index;
			//���� ������ parent ��� ��ȯ
			//�� ��� index �� root �� �޾Ƶ鿩�� �Ѵ�.
			tree.setRoot(index);
		}
		//�θ� ��尡 �����Ѵٸ�
		else
		{
			//�θ� ��尡 �� ���� �ʾҴٸ�
			if(this.parent.p.size() != this.parent.maxsize)
			{
				//�� ����� parent �� ���� ����� parent �� ����
				leaf.parent = this.parent;
				//���� parent ��忡 ���ο� (Ű,���)���� ����
				this.parent.insert(upperkey, leaf, tree);
			}
			//�θ� ��尡 �� ���ִٸ�
			else
			{
				IndexNode i_node = this.parent.split(upperkey, leaf, tree);
				for(int i=0; i<i_node.p.size(); i++)
				{
					i_node.p.get(i).getR().parent = i_node;
				}
				i_node.r.parent = i_node;
				//�ε���尡 full�� ��� end
			}
			//�θ��尡 �����ϴ� ��� end
		}
		//split end
	}
	//leafnode end		
}

class IndexNode extends Node
{
	public Node r;
	public List<PairIndex> p;
	
	//������
	public IndexNode(int degree)
	{
		super(degree);
		p = new ArrayList<PairIndex>();
		super.isLeaf = false;
	}
	//tree ��ȸ ���
	public void traverse()
	{
		System.out.print("[");
		for(int i=0; i<p.size(); i++)
		{
			System.out.print(p.get(i).getL() + ",");
		}
		System.out.println("]");
		for(int i=0; i<p.size(); i++)
		{
			p.get(i).getR().traverse();
		}
		r.traverse();
	}
	//leaf ��� ����� �˻�
	public Node searchLeaf(int key)
	{	
		for(int i=0; i<p.size(); i++)
		{
			//ã�� key ���� ū key�� ù��°�� �߰��� ��ġ
			//�ش� ��ġ�� pair���� node�� �����Ѵ�
			if(p.get(i).getL() > key)
			{
				return p.get(i).getR().searchLeaf(key);
			}
		}
		//ã�� key�� �ش� ��忡 �����ϴ� ��� key���� ū ��� rightmost ����
		return this.r.searchLeaf(key);
	}
	//�ش� ����� ã�� key�� �����ϴ��� check
	public int searchKey(int key)
	{
		for(int i=0; i<p.size(); i++)
		{
			if(p.get(i).getL() == key)
			{
				return i;
			}
		}
		return -1;
	}
	public int searchForInsert(int key)
	{
		if(this.p.size() == 0)
			return 0;
		for(int i=0; i<this.p.size(); i++)
		{
			//������ key���� ū key���� ���ʷ� �߰��� ��ġ ��ȯ
			if(this.p.get(i).getL() > key)
			{
				return i;
			}
		}
		//������ key���� ū key�� ���� ��� ���� ������ ���� ���� ����
		return this.p.size();
	}
	//leaf ��� ã���鼭 ��ġ�� ��� index key�� ���
	public void printSearch(int key)
	{
		for(int i=0; i<p.size(); i++)
		{
			System.out.print(p.get(i).getL() + ", ");
		}
		System.out.println("");
		int place = searchForInsert(key);
		if(place == p.size())
		{
			this.r.printSearch(key);
		}
		else
		{
			p.get(place).getR().printSearch(key);
		}
		//printSearch end
	}
	public IndexNode getleft(int position, IndexNode parent)
	{
		if(position == 0)
		{
			return null;
		}
		else
		{
			return (IndexNode) parent.p.get(position-1).getR();
		}
	}
	public IndexNode getright(int position, IndexNode parent)
	{
		if(position == parent.p.size())
		{
			return null;
		}
		else
		{
			if(position == parent.p.size()-1)
				return (IndexNode)parent.r;
			else
				return (IndexNode) parent.p.get(position+1).getR();
		}
	}
	//���� operation
	public void insert(int key, Node node, Bplustree tree)
	{
		// �ε��� ��尡 �� ���ִٸ�
		if(this.p.size() == maxsize)
		{
			split(key, node, tree);
		}
		// �ε��� ��尡 �� ������ �ʴٸ�
		else
		{
			//�ε��� pair ����
			PairIndex tmp = new PairIndex(key, node);
			
			//�ε��� pair�� ���Ե� ��ġ�� ã�� �� �ش� ��ġ�� ����.
			this.p.add(searchForInsert(key), tmp);
		}
	}
	public void delete(int place, Bplustree tree)
	{
		int half = maxsize/2;
		//�ε��� ����� ���Ұ����� ���� �ʰ��� case
		if(this.p.size() > half)
		{
			this.p.remove(place);
			return;
		}
		//�ε��� ����� ���Ұ����� ������ case
		else if(half == this.p.size())
		{
			//�켱 ����
			this.p.remove(place);
			
			//�ش� ��尡 root�� case 
			if(this.parent == null)
				return;
			
			//�θ��忡�� �ش� ��尡 ����� ��ġ = position
			int position = this.parent.searchForInsert(this.p.get(0).getL());
			IndexNode left = getleft(position, this.parent);
			IndexNode right = getright(position, this.parent);
			
			//���� ��� ���� case
			if(left != null)
			{
				//���� borrow
				if(left.p.size() > half)
				{
					//(�θ� ����� position ����Ű + ���� ����� rightmost child) pair �߰�
					int movekey = this.parent.p.get(position-1).getL();
					Node movenode = left.r;
					PairIndex tmp = new PairIndex(movekey, movenode);
					this.p.add(0, tmp);
					movenode.parent = this;
					//�θ� ����� position ���� Ű ����
					this.parent.p.get(position-1).setL(left.p.get(left.p.size()-1).getL());
					//���� ����� ���� ������ key, rightmost node ����.
					Node leftend = left.p.get(left.p.size()-1).getR();
					left.r = leftend;
					left.p.remove(left.p.size()-1);
					return;
				}
				//���� ��� half�� ���.
				//������ ��� �����ϴ� case
				else if(right!=null)
				{
					//������ borrow
					if(right.p.size() > half)
					{
						//(�θ� ����� position Ű + ������ ����� leftfirst child) pair �߰�
						int movekey = this.parent.p.get(position).getL();
						Node movenode = right.p.get(0).getR();
						PairIndex tmp = new PairIndex(movekey, this.r);
						this.p.add(tmp);
						this.r = movenode;
						movenode.parent = this;
						//�θ� ����� position Ű ����
						this.parent.p.get(position).setL(right.p.get(0).getL());
						//������ ����� ù��° pair ����.
						right.p.remove(0);
						return;
					}
					//���� merge
					//���� ������ ��� ��� half
					else
					{
						//position-1 ��ġ�� �θ��� Ű���� ������ �´�
						int movekey = this.parent.p.get(position-1).getL();
						
						//left�� rightmost���� moveŰ�� �� pair ����
						PairIndex tmp = new PairIndex(movekey, left.r);
						left.p.add(tmp);
						
						//���� ��忡�� left ���� ���� ��� �̵�
						for(int i =0; i<half-1; i++)
						{
							PairIndex temp = this.p.remove(0);
							left.p.add(temp);
							temp.getR().parent = left;
						}
						
						//��������� rightmost���� left�� rightmost�� ����.
						left.r = this.r;
						left.r.parent = left;
						
						//�θ��忡�� �ش� Ű�� ����(delete)
						left.parent.p.get(position).setR(left);
						left.parent.delete(position-1, tree);
						return;
						//���� merge end
					}
					//������ ��� �����ϴ� case end
				}
				//���� ���� half, ������ ���� ���� case
				//���� ������ index ����� case
				//���� merge
				else
				{
					//position-1 ��ġ�� �θ��� Ű���� ������ �´�
					int movekey = this.parent.p.get(position-1).getL();
					
					//left�� rightmost���� moveŰ�� �� pair ����
					PairIndex tmp = new PairIndex(movekey, left.r);
					left.p.add(tmp);
					
					//���� ��忡�� left ���� ���� ��� �̵�
					for(int i =0; i<half-1; i++)
					{
						PairIndex temp = this.p.remove(0);
						left.p.add(temp);
						temp.getR().parent = left;
					}
					
					//��������� rightmost���� left�� rightmost�� ����.
					left.r = this.r;
					left.r.parent = left;
					
					//�θ��忡�� �ش� Ű�� ����(delete)
					left.parent.r = left;
					left.parent.delete(position-1, tree);
					return;
					//������ �ε��� ��� ���� merge end
				}
				//���� ��� ���� case end
			}
			//���� ��� �������� �ʰ� ������ ��� �����ϴ� case
			//���� ���� index����� case
			else if(right != null)
			{
				//������ borrow
				if(right.p.size() > half)
				{
					//(�θ� ����� position Ű + ������ ����� leftfirst child) pair �߰�
					int movekey = this.parent.p.get(position).getL();
					Node movenode = right.p.get(0).getR();
					PairIndex tmp = new PairIndex(movekey, this.r);
					this.p.add(tmp);
					this.r = movenode;
					movenode.parent = this;
					//�θ� ����� position Ű ����
					this.parent.p.get(position).setL(right.p.get(0).getL());
					//������ ����� ù��° pair ����.
					right.p.remove(0);
					return;
					//������ borrow end
				}
				//������ merge, ������ ��尡 half�� ���
				else
				{
					//position ��ġ�� �θ��� Ű���� ������ �´�
					int movekey = this.parent.p.get(position).getL();
					
					//�������� rightmost���� moveŰ�� �� pair ����, right��忡 �߰�
					PairIndex tmp = new PairIndex(movekey, this.r);
					right.p.add(0,tmp);
					this.r.parent = right;
					
					//���� ��忡�� right ���� ���� ��� �̵�
					for(int i =half-2; i>=0; i--)
					{
						PairIndex temp = this.p.remove(i);
						right.p.add(0,temp);
						temp.getR().parent = right;
					}
					
					//�θ��忡�� �ش� Ű�� ����(delete)
					right.parent.delete(position, tree);
					return;
					//������ merge end
				}
				//���� ó���� �ε��� ��� case end
			}
			//���Ұ����� half���� case end
		}
		//���Ұ����� half���� ���� ���, root�� ����̴�.
		else if(this.parent == null)
		{
			//�켱 �����Ѵ�
			this.p.remove(place);
			
			//root�� ���Ұ� 1���ۿ� ���� ��� 
			if(this.p.size() == 1)
			{
				//�ڽĵ��� leaf ����� case
				if(this.r.isLeaf)
				{
					LeafNode rightchild = (LeafNode)this.r;
					tree.setRoot(rightchild);
					return;
				}
				//�ڽĵ��� index ����� case
				else
				{
					IndexNode rightindex = (IndexNode)this.r;
					tree.setRoot(rightindex);
					return;
				}
				//root ���� 1�� case end
			}
			//root ���Ұ� 1������ ���� half���� ���� case
			else
				return;
			//half���� ���� case end
		}
		//end delete
	}
	public IndexNode split(int key, Node node, Bplustree tree)
	{
		//�ε��� pair ����
		PairIndex tmp = new PairIndex(key, node);
		
		//�ε��� ��� list �� �ε��� pair ����&����
		//���� maxsize���� ���Ұ����� �ϳ� �� ����
		this.p.add(searchForInsert(key), tmp);
		
		//�ε��� ��� ����
		IndexNode index = new IndexNode(maxsize);
		int half = gethalf();
		
		//�տ������� half ������ŭ�� ���ҵ��� �� �ε��� ���� �̵�
		for(int i=0; i<half; i++)
		{
			//remove first element
			PairIndex pairindex = p.remove(0);
			//put first element into new node
			index.p.add(pairindex);
		}
		//�� �ε��� ����� rightmost child�� ���� �ε��� ����� leftmost pair�� ��带 �̵�
		//���� �ε��� ����� leftmost pair�� Ű���� �θ� �ε��� ���� �ø��� �ش���ġ���� ����
		index.r = this.p.get(0).getR();
		int upperkey = this.p.get(0).getL();
		this.p.remove(0);
		
		//���� parent �ε����� ���ٸ� ���� ����� index ��尡 root�� �ȴ�
		if(this.parent == null)
		{
			//�� root ��� ����
			IndexNode inode = new IndexNode(maxsize);
			
			//�� �ε��� pair ���� �� �� root ��忡 ����
			//�� root ����� rightmost child �� ���� ��� ����.
			PairIndex pindex = new PairIndex(upperkey, index);
			inode.p.add(pindex);
			inode.r = this;
			
			//���� ���� ���� ����� parent root �� ����
			index.parent = inode;
			this.parent = inode;
			
			//b plus tree �� root �� ��ġ�� �������ش�
			tree.setRoot(inode);
		}
		
		//���� parent node �����Ѵٸ�
		else
		{
			//�θ� ��尡 �� ���� �ʾҴٸ�
			if(this.parent.p.size() != this.parent.maxsize)
			{
				//�� ����� parent �� ���� ����� parent �� ����
				//���� parent ��忡 ���ο� (���ο� Ű, �� �ε��� ���)���� ����
				index.parent = this.parent;
				this.parent.insert(upperkey, index, tree);
			}
			
			//�θ� ��尡 �� ���ִٸ�
			else
			{
				IndexNode i_node = this.parent.split(upperkey, index, tree);
				for(int i=0; i<i_node.p.size(); i++)
				{
					i_node.p.get(i).getR().parent = i_node;
				}
				i_node.r.parent = i_node;
				//parent ��� �� �� �ִٸ� end
			}
			//parent ��� �����Ѵٸ� end
		}
		//�� index ��带 ��ȯ���־ parent ������ �����ش�
		return index;
		//split end
	}
	//index class end
}

class Bplustree{
	public Node root;
	public int maxsize;
	
	public Bplustree(int degree)
	{
		root = null;
		this.maxsize = degree;
	}
	public void setRoot(LeafNode leaf)
	{
		this.root = leaf;
		this.root.parent = null;
	}
	public void setRoot(IndexNode index)
	{
		this.root = index;
		this.root.parent = null;
	}
	
	public void insertion(List<PairLeaf> list)
	{
		for(int i=0; i<list.size(); i++)
		{
			PairLeaf pair = list.get(i);
			//insertion
			if(root == null)
			{
				LeafNode leaf = new LeafNode(maxsize);
				leaf.insert(pair.getL(), pair.getR(), this);
				setRoot(leaf);
			}
			else
			{
				LeafNode tmp = (LeafNode) root.searchLeaf(pair.getL());
				tmp.insert(pair.getL(), pair.getR(), this);
			}
			//for end
		}
	}
	public void deletion(List<Integer> list)
	{
		if(root == null)
		{
			System.out.println("tree is empty!!");
			return;
		}
		for(int i=0; i<list.size(); i++)
		{
			int key = list.get(i);
			LeafNode leaf = (LeafNode)root.searchLeaf(key);
			leaf.delete(key, this);
		}
	}
	public void search(int key)
	{
		root.printSearch(key);
	}
	public void rsearch(int start, int finish)
	{
		LeafNode startLeaf = (LeafNode) root.searchLeaf(start);
		int s_point = 0;
		for(s_point=0; s_point<startLeaf.p.size(); s_point++)
		{
			if((startLeaf.p.get(s_point).getL()<=finish) && (startLeaf.p.get(s_point).getL()>=start))
				break;
			if(s_point == startLeaf.p.size() - 1)
			{
				System.out.println("NOT FOUND");
				return;
			}
		}
		LeafNode leaf = startLeaf;
		int key = leaf.p.get(s_point).getL();
		while(key <= finish)
		{
			if(s_point == leaf.p.size() - 1)
			{
				System.out.println(leaf.p.get(s_point).getL() + ", " + leaf.p.get(s_point).getR());
				if(leaf.r != null)
				{
					leaf = (LeafNode) leaf.r;
				}
				else
				{
					return;
				}
				s_point = 0;
				key = leaf.p.get(s_point).getL();
				//if end
			}
			else
			{
				System.out.println(leaf.p.get(s_point).getL() + ", " + leaf.p.get(s_point).getR());
				key = leaf.p.get(++s_point).getL();
				//else end
			}
			//while end
		}
	}
}



public class bptree {
//	private static Node root;
	
	public static void main(String[] args) {
		//make elements
		List<PairLeaf> list = new ArrayList<PairLeaf>();
		List<Integer> list2 = new ArrayList<Integer>();
		List<Integer> list3 = new ArrayList<Integer>();
		Bplustree tree = new Bplustree(10);
		for(int i=1000000; i>0; i--)
		{
			PairLeaf tmp = new PairLeaf(i, i+1000);
			list.add(tmp);
		}
		for(int i=890; i<910; i++)
		{
			list2.add(i);
		}
		for(int i=120; i<200; i++)
		{
			list3.add(i);
		}
		tree.insertion(list);
		tree.deletion(list2);
		tree.search(900);
	}
}


