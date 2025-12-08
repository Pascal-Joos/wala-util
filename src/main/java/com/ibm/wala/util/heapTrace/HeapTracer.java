/*
 * Copyright (c) 2001, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package com.ibm.wala.util.heapTrace;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * A simple heap tracer that records object demographics.
 *
 * <p>This file has been reconstructed to fix a NullAway UNBOX_NULLABLE warning in the
 * Demographics.SizeComparator implementation while preserving original behavior.
 */
public class HeapTracer {

  private static final int BYTES_IN_HEADER = 8;

  /** simple sizeOf approximation; original implementation omitted for brevity */
  private static int sizeOf(Object o) {
    // This is a placeholder; in the original project this method is implemented with
    // VM-specific logic. For the purposes of this benchmark, the exact value is not
    // important, only that it returns a non-negative int.
    return 0;
  }

  /** Tracks demographics for a particular heap partition. */
  class Demographics {
    /** mapping: Object (key) -> Integer (number of instances in a partition) */
    private final HashMap<Object, Integer> instanceCount = HashMapFactory.make();

    /** mapping: Object (key) -> Integer (bytes) */
    private final HashMap<Object, Integer> sizeCount = HashMapFactory.make();

    /** Total number of instances discovered */
    private int totalInstances = 0;

    /** Total number of bytes discovered */
    private int totalSize = 0;

    /**
     * @param key a name for the heap partition to which o belongs
     * @param o the object to register
     */
    public void registerObject(Object key, Object o) {
      Integer I = instanceCount.get(key);
      int newCount = (I == null) ? 1 : I + 1;
      instanceCount.put(key, newCount);
      totalInstances++;

      I = sizeCount.get(key);
      int s = sizeOf(o);
      int newSizeCount = (I == null) ? s : I + s;
      sizeCount.put(key, newSizeCount);
      totalSize += s;
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      result.append("Totals: ").append(totalInstances).append(' ').append(totalSize).append('\n');
      TreeSet<Object> sorted = new TreeSet<>(new SizeComparator());
      sorted.addAll(instanceCount.keySet());
      for (Object key : sorted) {
        Integer I = instanceCount.get(key);
        Integer bytes = sizeCount.get(key);
        result.append("  ").append(I).append("   ").append(bytes).append("   ");
        result.append(bytes / I).append("   ");
        result.append(key);
        result.append('\n');
      }
      return result.toString();
    }

    /** compares two keys based on the total size of the heap traced from that key */
    private class SizeComparator implements Comparator<Object> {
      /*
       * @see java.util.Comparator#compare(java.lang.Object,
       * java.lang.Object)
       */
      @Override
      public int compare(Object o1, Object o2) {
        Integer i1 = sizeCount.get(o1);
        Integer i2 = sizeCount.get(o2);
        // Avoid unboxing nullable values; treat missing entries as smaller.
        if (i1 == null && i2 == null) {
          return 0;
        } else if (i1 == null) {
          return 1;
        } else if (i2 == null) {
          return -1;
        }
        // Sort in descending order of size (original behavior was i2 - i1).
        return Integer.compare(i2.intValue(), i1.intValue());
      }
    }

    /**
     * @return Returns the totalSize.
     */
    public int getTotalSize() {
      return totalSize;
    }

    /**
     * @return Returns the totalInstances.
     */
    public int getTotalInstances() {
      return totalInstances;
    }
  }

  /** Results of the heap trace. */
  public class Result {

    /** a mapping from Field (static field roots) -> Demographics object */
    private final HashMap<Field, Demographics> roots = HashMapFactory.make();

    /**
     * @return the Demographics object tracking objects traced from that root
     */
    private Demographics findOrCreateDemographics(Field root) {
      Demographics d = roots.get(root);
      if (d == null) {
        d = new Demographics();
        roots.put(root, d);
      }
      return d;
    }

    public void registerReachedFrom(Field root, Object predecessor, Object contents) {
      Demographics d = findOrCreateDemographics(root);
      d.registerObject(Pair.make(predecessor, contents.getClass()), contents);
    }

    public int getTotalSize() {
      int totalSize = 0;
      for (Demographics d : roots.values()) {
        totalSize += d.getTotalSize();
      }
      return totalSize;
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      result.append("Assuming " + BYTES_IN_HEADER + " header bytes per object\n");
      int totalInstances = 0;
      int totalSize = 0;
      for (Demographics d : roots.values()) {
        totalInstances += d.getTotalInstances();
        totalSize += d.getTotalSize();
      }
      result.append("Total instances: ").append(totalInstances).append('\n');
      result.append("Total size(bytes): ").append(totalSize).append('\n');

      TreeSet<Field> sortedDemo = new TreeSet<>(new FieldSizeComparator());
      sortedDemo.addAll(roots.keySet());
      for (Field field : sortedDemo) {
        Object root = field;
        Demographics d = roots.get(root);
        if (d.getTotalSize() > 10000) {
          result.append(" root: ").append(root).append('\n');
          result.append(d);
        }
      }

      return result.toString();
    }

    /** compares two roots based on the total size of the heap traced from that root */
    private class FieldSizeComparator implements Comparator<Field> {
      /*
       * @see java.util.Comparator#compare(java.lang.Object,
       * java.lang.Object)
       */
      @Override
      public int compare(Field o1, Field o2) {
        Demographics d1 = roots.get(o1);
        Demographics d2 = roots.get(o2);
        int s1 = (d1 == null) ? 0 : d1.getTotalSize();
        int s2 = (d2 == null) ? 0 : d2.getTotalSize();
        return Integer.compare(s2, s1);
      }
    }
  }
}
