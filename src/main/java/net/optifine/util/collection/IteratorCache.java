package net.optifine.util.collection;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public class IteratorCache {
	private static final Deque<IteratorReusable<Object>> dequeIterators = new ArrayDeque<>();

	static {
		for (int i = 0; i < 1000; ++i) {
			dequeIterators.add(new IteratorReadOnly());
		}
	}

	public static Iterator<Object> getReadOnly(List list) {
		synchronized (dequeIterators) {
			IteratorReusable<Object> iteratorreusable = dequeIterators.pollFirst();

			if (iteratorreusable == null) {
				iteratorreusable = new IteratorReadOnly();
			}

			iteratorreusable.setList(list);
			return iteratorreusable;
		}
	}

	private static void finished(IteratorReusable<Object> iterator) {
		synchronized (dequeIterators) {
			if (dequeIterators.size() <= 1000) {
				iterator.setList(null);
				dequeIterators.addLast(iterator);
			}
		}
	}

	public interface IteratorReusable<E> extends Iterator<E> {
		void setList(List<E> var1);
	}

	public static class IteratorReadOnly implements IteratorReusable<Object> {
		private List<Object> list;
		private int index;
		private boolean hasNext;

		public void setList(List<Object> list) {
			if (this.hasNext) {
				throw new RuntimeException("Iterator still used, oldList: " + this.list + ", newList: " + list);
			} else {
				this.list = list;
				this.index = 0;
				this.hasNext = list != null && this.index < list.size();
			}
		}

		public Object next() {
			if (!this.hasNext) {
				return null;
			} else {
				Object object = this.list.get(this.index);
				++this.index;
				this.hasNext = this.index < this.list.size();
				return object;
			}
		}

		public boolean hasNext() {
			if (!this.hasNext) {
				finished(this);
				return false;
			} else {
				return this.hasNext;
			}
		}

		public void remove() {
			throw new UnsupportedOperationException("remove");
		}
	}
}
