/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.intset;

import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** A Set backed by a set of integers. */
public class OrdinalSet<T> implements Iterable<T> {

  @Nullable private final IntSet S;

  private final OrdinalSetMapping<T> mapping;

  @SuppressWarnings("rawtypes")
  private static final OrdinalSet EMPTY = new OrdinalSet();

  /**
   * A dummy mapping used for the singleton empty set. It should never actually be invoked, since
   * the empty set has no elements and its iterator is always empty.
   */
  private static final OrdinalSetMapping<Object> EMPTY_MAPPING =
      new OrdinalSetMapping<Object>() {
        @Override
        public Object getMappedObject(int n) throws NoSuchElementException {
          throw new UnsupportedOperationException("EMPTY OrdinalSet has no mapping");
        }

        @Override
        public int getMappedIndex(Object o) {
          return -1;
        }

        @Override
        public boolean hasMappedIndex(Object o) {
          return false;
        }

        @Override
        public int getMaximumIndex() {
          return -1;
        }

        @Override
        public int getSize() {
          return 0;
        }

        @Override
        public int add(Object o) {
          throw new UnsupportedOperationException("EMPTY OrdinalSet has no mapping");
        }

        @Override
        public Stream<Object> stream() {
          return Stream.empty();
        }

        @Override
        public Iterator<Object> iterator() {
          return EmptyIterator.instance();
        }
      };

  public static <T> OrdinalSet<T> empty() {
    return EMPTY;
  }

  @SuppressWarnings("unchecked")
  private OrdinalSet() {
    S = null;
    mapping = (OrdinalSetMapping<T>) EMPTY_MAPPING;
  }

  public OrdinalSet(@Nullable IntSet S, OrdinalSetMapping<T> mapping) {
    this.S = S;
    this.mapping = mapping;
  }

  public boolean containsAny(OrdinalSet<T> that) {
    if (that == null) {
      throw new IllegalArgumentException("null that");
    }
    if (S == null || that.S == null) {
      return false;
    }
    return S.containsAny(that.S);
  }

  public int size() {
    return (S == null) ? 0 : S.size();
  }

  @Override
  public Iterator<T> iterator() {
    if (S == null) {
      return EmptyIterator.instance();
    } else {

      return new Iterator<T>() {
        final IntIterator it = S.intIterator();

        @Override
        public boolean hasNext() {
          return it.hasNext();
        }

        @Override
        public T next() {
          return mapping.getMappedObject(it.next());
        }

        @Override
        public void remove() {
          Assertions.UNREACHABLE();
        }
      };
    }
  }

  /**
   * @return a new OrdinalSet instances
   * @throws IllegalArgumentException if A is null
   */
  public static <T> OrdinalSet<T> intersect(OrdinalSet<T> A, OrdinalSet<T> B) {
    if (A == null) {
      throw new IllegalArgumentException("A is null");
    }
    if (A.size() != 0 && B.size() != 0) {
      assert A.mapping.equals(B.mapping);
    }
    if (A.S == null || B.S == null) {
      return new OrdinalSet<>(null, A.mapping);
    }
    IntSet isect = A.S.intersection(B.S);
    return new OrdinalSet<>(isect, A.mapping);
  }

  /**
   * @return true if the contents of two sets are equal
   */
  public static <T> boolean equals(OrdinalSet<T> a, OrdinalSet<T> b) {
    if ((a == null && b == null) || a == b || (a.mapping == b.mapping && a.S == b.S)) {
      return true;
    }

    assert a != null && b != null;
    if (a.size() == b.size()) {
      if (a.mapping == b.mapping || a.mapping.equals(b.mapping)) {
        return a.S == b.S || (a.S != null && b.S != null && a.S.sameValue(b.S));
      }
    }

    return false;
  }

  /**
   * Creates the union of two ordinal sets.
   *
   * @param A ordinal set a
   * @param B ordinal set b
   * @return union of a and b
   * @throws IllegalArgumentException iff A or B is null
   */
  public static <T> OrdinalSet<T> unify(OrdinalSet<T> A, OrdinalSet<T> B) {
    if (A == null) {
      throw new IllegalArgumentException("A is null");
    }
    if (B == null) {
      throw new IllegalArgumentException("B is null");
    }
    if (A.size() != 0 && B.size() != 0) {
      assert A.mapping.equals(B.mapping);
    }

    if (A.S == null) {
      return (B.S == null) ? OrdinalSet.<T>empty() : new OrdinalSet<>(B.S, B.mapping);
    } else if (B.S == null) {
      return new OrdinalSet<>(A.S, A.mapping);
    }

    IntSet union = A.S.union(B.S);
    return new OrdinalSet<>(union, A.mapping);
  }

  @Override
  public String toString() {
    return Iterator2Collection.toSet(iterator()).toString();
  }

  /** */
  public SparseIntSet makeSparseCopy() {
    return (S == null) ? new SparseIntSet() : new SparseIntSet(S);
  }
}
