package util;

import java.util.ArrayList;

public class ArraySet<E> extends ArrayList<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public boolean add(E e) {
		if (this.contains(e) == false) {
			return super.add(e);
		}

		return true;
	}

}
