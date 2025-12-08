package com.ibm.wala.util.heapTrace;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;

public class HeapTracer {

  private static final int BYTES_IN_HEADER = 8;

  private final HashMap<Object, Demographics> roots = new HashMap<>();

  public void addRoot(Object root) {
    if (!roots.containsKey(root)) {
      roots.put(root, new Demographics());
    }
  }

  public void addInstance(Object root, int size) {
    Demographics d = roots.get(root);
    if (d == null) {
      d = new Demographics();
      roots.put(root, d);
    }
    d.addInstance(size);
  }

  public void dump(PrintStream out) {
    out.println(toString());
  }

  private static class Demographics {

    private int totalInstances;

    private int totalSize;

    public void addInstance(int size) {
      totalInstances++;
      totalSize += size + BYTES_IN_HEADER;
    }

    public int getTotalInstances() {
      return totalInstances;
    }

    public int getTotalSize() {
      return totalSize;
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      result.append("  instances: ").append(totalInstances).append('\n');
      result.append("  total size(bytes): ").append(totalSize).append('\n');
      return result.toString();
    }
  }

  public int getTotalInstances() {
    int totalInstances = 0;
    for (Demographics d : roots.values()) {
      totalInstances += d.getTotalInstances();
    }
    return totalInstances;
  }

  public int getTotalSize() {
    int totalSize = 0;
    for (Demographics d : roots.values()) {
      totalSize += d.getTotalSize();
    }
    return totalSize;
  }

  /**
   * Helper to safely obtain the total size for a given root key. If the key is not present in the
   * roots map, we treat its size as 0 to avoid dereferencing a null Demographics.
   */
  private int getTotalSize(Object key) {
    Demographics d = roots.get(key);
    return d == null ? 0 : d.getTotalSize();
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

    TreeSet<Object> sortedDemo = new TreeSet<>(new SizeComparator());
    sortedDemo.addAll(roots.keySet());
    for (Object root : sortedDemo) {
      Demographics d = roots.get(root);
      if (d == null) {
        continue;
      }
      if (d.getTotalSize() > 10000) {
        result.append(" root: ").append(root).append('\n');
        result.append(d);
      }
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
      // Use the null-safe helper to avoid dereferencing a potentially null Demographics
      return getTotalSize(o2) - getTotalSize(o1);
    }
  }
}
