/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.test.metric.collection;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class acts as a stack externally. The difference is that internally it
 * is implemented with several stacks growing in parallel. The stack starts as a
 * single stack when it is constructed. As it grows it can be split into
 * multiple keys. The keys can then be rejoined to a new key. Any push/pop
 * operations will be performed on one or more internal stacks depending on how
 * the stack was split and rejoined.
 * 
 * This is useful when analyzing stack machines (such as Java JVM) which keep
 * variables on the stack but whose execution can split and rejoin on the level
 * of byte-codes.
 * 
 * 
 * @author mhevery@google.com <Misko Hevery>
 * 
 * @param <KEY>
 *            Selector which decides which of the stacks are being pushed /
 *            popped
 * @param <VALUE>
 *            Value on stack.
 */
public class KeyedMultiStack<KEY, VALUE> {

	public static class StackUnderflowException extends RuntimeException {
		private static final long serialVersionUID = 4649233306901482842L;
	}

	public static class KeyNotFoundException extends RuntimeException {
		private static final long serialVersionUID = 4649233306901482842L;

		public <KEY> KeyNotFoundException(KEY key) {
			super("Key '" + key + "' not found.");
		}
	}

	private static class Entry<VALUE> {
		private final int depth;
		private final List<Entry<VALUE>> parents;
		private final VALUE value;

		private Entry() {
			this.depth = -1;
			this.parents = null;
			this.value = null;
		}

		private Entry(List<Entry<VALUE>> parents, VALUE value) {
			this.value = value;
			this.parents = parents;
			this.depth = parents.size() == 0 ? -1
					: parents.iterator().next().depth + 1;
		}

		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			buf.append("\n");
			toString(buf, "");
			return buf.toString();
		}

		private void toString(StringBuilder buf, String offset) {
			if (depth == -1) {
				return;
			}
			buf.append(offset);
			buf.append("#");
			buf.append(depth);
			buf.append("(");
			buf.append(value);
			buf.append(")\n");
			for (Entry<VALUE> parent : parents) {
				parent.toString(buf, offset + "  ");
			}
		}

		public List<Entry<VALUE>> getParents() {
			if (depth == -1) {
				throw new StackUnderflowException();
			}
			return parents;
		}

	}

	private Entry<VALUE> root = new Entry<VALUE>();

	private Map<KEY, List<Entry<VALUE>>> head = new HashMap<KEY, List<Entry<VALUE>>>();

	public KeyedMultiStack() {
	}

	/**
	 * @param key
	 *            Initial key for the primordial stack.
	 */
	public KeyedMultiStack(KEY key) {
		init(key);
	}

	public void init(KEY key) {
		head.put(key, set(root));
	}

	/**
	 * Push a value on the stack
	 * 
	 * @param key
	 *            which could represent more then one stack.
	 * @param value
	 *            value to push
	 */
	public void push(KEY key, VALUE value) {
		List<Entry<VALUE>> parents = removeHead(key);
		head.put(key, set(new Entry<VALUE>(parents, value)));
	}

	private List<Entry<VALUE>> getHead(KEY key) {
		if (!head.containsKey(key)) {
			throw new KeyNotFoundException(key);
		} else {
			return head.get(key);
		}
	}

	private List<Entry<VALUE>> removeHead(KEY key) {
		if (!head.containsKey(key)) {
			throw new KeyNotFoundException(key);
		} else {
			return head.remove(key);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Entry<VALUE>> set(Entry<VALUE> entries) {
		return new LinkedList<Entry<VALUE>>(asList(entries));
	}

	/**
	 * Pop vale from the stack. The closure can be called more then once if
	 * there are parallel stacks due to splits.
	 * 
	 * @param key
	 *            key as stack selector
	 * @param size
	 *            Number of elements to pop
	 * @param popClosure
	 *            Closer which will be called once per each virtual stack.
	 */
	public void pop(KEY key, int size, PopClosure<KEY, VALUE> popClosure) {
		List<List<VALUE>> paths = fillPopPaths(getHead(key), size);
		popPaths(key, size);
		for (List<VALUE> path : paths) {
			popClosure.pop(key, path);
		}
	}

	private void popPaths(KEY key, int size) {
		if (size == 0) {
			return;
		}
		List<Entry<VALUE>> newEntries = new LinkedList<Entry<VALUE>>();
		for (Entry<VALUE> entry : getHead(key)) {
			newEntries.addAll(entry.getParents());
		}
		head.put(key, newEntries);
		popPaths(key, size - 1);
	}

	private List<List<VALUE>> fillPopPaths(List<Entry<VALUE>> entries, int size) {
		List<List<VALUE>> paths = new LinkedList<List<VALUE>>();
		if (size == 0) {
			LinkedList<VALUE> path = new LinkedList<VALUE>();
			paths.add(path);
		} else {
			for (Entry<VALUE> entry : entries) {
				if (entry.depth < size - 1) {
					throw new StackUnderflowException();
				}
				for (List<VALUE> path : fillPopPaths(entry.getParents(), size - 1)) {
					path.add(entry.value);
					paths.add(path);
				}
			}
		}
		return paths;
	}

	/**
	 * Split the internal stacks to a new set of stacks
	 * 
	 * @param key
	 *            Stack(s) to split.
	 * @param subKeys
	 *            New names for those stacks
	 */
	public void split(KEY key, List<KEY> subKeys) {
		List<Entry<VALUE>> entries = removeHead(key);
		for (KEY subKey : subKeys) {
			if (head.containsKey(subKey)) {
				List<Entry<VALUE>> existingList = head.get(subKey);
				entries.removeAll(existingList); // Don't want duplicates
				entries.addAll(existingList);
			}
		}
		assertSameDepth(entries);
		for (KEY subKey : subKeys) {
			head.put(subKey, entries);
		}
	}

	/**
	 * Rejoin the stacks. This is not always possible since each stack may now
	 * be different, but it will make sure that the new single key will now
	 * refer to all of the stacks treating them as one. This will make the pop
	 * operation be applied to individual stacks.
	 * 
	 * @param subKeys
	 *            a list of keys for the old stacks to join.
	 * @param newKey
	 *            new name for the stack
	 */
	public void join(List<KEY> subKeys, KEY newKey) {
		List<Entry<VALUE>> newHead = new LinkedList<Entry<VALUE>>();
		for (KEY key : subKeys) {
			List<Entry<VALUE>> entries = getHead(key);
			newHead.removeAll(entries); // Remove any duplicates
			newHead.addAll(entries); // Add everything once.
		}
		assertSameDepth(newHead);
		for (KEY key : subKeys) {
			removeHead(key);
		}
		head.put(newKey, newHead);
	}

	private void assertSameDepth(List<Entry<VALUE>> entries) {
		int expectedDepth = -1;
		for (Entry<VALUE> entry : entries) {
			if (expectedDepth == -1) {
				expectedDepth = entry.depth;
			} else if (expectedDepth != entry.depth) {
				throw new IllegalStateException(
						"Not all entries are at same depth. Can't join.");
			}
		}
	}

	public void assertEmpty() {
		for (List<Entry<VALUE>> entries : head.values()) {
			for (Entry<VALUE> entry : entries) {
				if (entry.depth > -1) {
					throw new IllegalStateException("Stack not empty.");
				}
			}
		}
	}

	@Override
	public String toString() {
		return head.toString();
	}

}
