package edu.stanford.muse.ie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.muse.util.Util;

/** little class to let us pretty toString type hierarchies */
public class TypeHierarchy {
	private final Map<String, TypeHierarchyNode>	dir	= new LinkedHashMap<>();

	class TypeHierarchyNode implements Comparable<TypeHierarchyNode> {
		int						count, ownCount;	// owncount does not include children's count
		String					type;
		final List<TypeHierarchyNode>	children;
		TypeHierarchyNode		parent;

		private TypeHierarchyNode() {
			children = new ArrayList<>();
		}

		@Override
		public int hashCode() {
			return type.hashCode();
		}

		@Override
		public boolean equals(Object o)
		{
			if (o == null)
				return false;
			return ((TypeHierarchyNode) o).type.equals(this.type);
		}

		@Override
		public int compareTo(TypeHierarchyNode arg0) {
			return arg0.count - this.count;
			// TODO Auto-generated method stub
		}
	}

	// e.g. s = A|B|C, count = 3
	public void recordTypeCount(String s, int count)
	{
		List<String> tokens = Util.tokenize(s, "|");
		TypeHierarchyNode leaf = null;
		while (tokens.size() > 0)
		{
			String child = Util.join(tokens, "|");
			tokens.remove(0);
			TypeHierarchyNode parentNode = null;
			if (tokens.size() > 0)
			{
				String parent = Util.join(tokens, "|");
				parentNode = nodeFor(parent);
			}

			TypeHierarchyNode childNode = nodeFor(child);
			childNode.parent = parentNode;
			if (parentNode != null && !parentNode.children.contains(childNode))
				parentNode.children.add(childNode);
			if (leaf == null)
				leaf = childNode;
		}

		leaf.ownCount += count;

		for (TypeHierarchyNode node = leaf; node != null; node = node.parent)
			node.count += count;
	}

	private String toString(TypeHierarchyNode t, int indent, boolean asHTML)
	{
		String newLine = asHTML ? "<br/>\n" : "\n";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++)
			sb.append(asHTML ? "&nbsp;" : " ");

		if (asHTML && indent == 0)
			sb.append("- ");

		if (asHTML && t.ownCount > 0) {
			//added to be able to select classes for seed instance generation.
			sb.append("<input type=\"checkbox\" data-type=\"" + t.type + "\"/>");
			sb.append("<a href=\"#" + t.type + "\">");
		}

		sb.append(t.type.replaceAll("\\|.*", ""));
		if (asHTML && t.ownCount > 0)
			sb.append("</a>");
		sb.append(" (" + t.count + ")" + newLine);

		if (t.children != null)
			for (TypeHierarchyNode t1 : t.children)
				sb.append(toString(t1, indent + 8, asHTML));
		return sb.toString();
	}

	// factory
	private TypeHierarchyNode nodeFor(String s)
	{
		if (Util.nullOrEmpty(s))
			Util.breakpoint();

		TypeHierarchyNode t = dir.get(s);
		if (t != null)
			return t;

		t = new TypeHierarchyNode();
		t.type = s;
		dir.put(s, t);
		return t;
	}

	private List<TypeHierarchyNode> getRootNodes()
	{
		List<TypeHierarchyNode> roots = new ArrayList<>();
		for (TypeHierarchyNode t : dir.values())
			if (t.parent == null)
				roots.add(t);
		return roots;
	}

	public String toString(boolean asHTML)
	{
		List<TypeHierarchyNode> roots = getRootNodes();
		Collections.sort(roots);
		StringBuilder sb = new StringBuilder();
		for (TypeHierarchyNode r : roots)
			sb.append(toString(r, 0, asHTML));
		return sb.toString();
	}

}