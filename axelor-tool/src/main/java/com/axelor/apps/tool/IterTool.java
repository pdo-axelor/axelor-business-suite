package com.axelor.apps.tool;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

public class IterTool {

  private IterTool() {}

  public static <T> Collection<T> getCollection(Collection<T> collection) {
    return collection != null ? collection : Collections.emptyList();
  }

  public static <T> Stream<T> getStream(Collection<T> collection) {
    return getCollection(collection).stream();
  }

  public static <T> Stream<T> getParallelStream(Collection<T> collection) {
    return getCollection(collection).parallelStream();
  }
}
