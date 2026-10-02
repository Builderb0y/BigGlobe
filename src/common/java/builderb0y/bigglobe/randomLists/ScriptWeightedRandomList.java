package builderb0y.bigglobe.randomLists;

import java.util.List;

import builderb0y.bigglobe.columns.ScriptedColumn;
import builderb0y.bigglobe.randomLists.IRandomList.RandomAccessRandomList;

public class ScriptWeightedRandomList<E extends IScriptWeightedListElement> extends AbstractRandomList<E> implements RandomAccessRandomList<E> {

	public List<E> elements;
	public ScriptedColumn column;
	public int y;

	public ScriptWeightedRandomList(List<E> elements, ScriptedColumn column, int y) {
		this.elements = elements;
		this.column = column;
		this.y = y;
	}

	@Override
	public E get(int index) {
		return this.elements.get(index);
	}

	@Override
	public double getWeight(int index) {
		return this.elements.get(index).getRestrictedWeight(this.column, this.y);
	}

	@Override
	public int size() {
		return this.elements.size();
	}
}