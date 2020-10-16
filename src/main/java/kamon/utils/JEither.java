package kamon.utils;

public final class JEither<L, R> {
  public static <L, R> JEither<L, R> left(L value) {
    return new JEither<L, R>(value, null);
  }

  public static <L, R> JEither<L, R> right(R value) {
    return new JEither<>(null, value);
  }

  private final L left;
  private final R right;

  private JEither(L l, R r) {
    left = l;
    right = r;
  }

  public L left() {
    return left;
  }

  public R right() {
    return right;
  }
}

//final class JEither<L, R> {
//  public static <L, R> JEither<L, R> left(L value) {
//    return new JEither<>(Optional.of(value), Optional.empty());
//  }
//
//  public static <L, R> JEither<L, R> right(R value) {
//    return new JEither<>(Optional.empty(), Optional.of(value));
//  }
//
//  private final Optional<L> left;
//  private final Optional<R> right;
//
//  private JEither(Optional<L> l, Optional<R> r) {
//    left = l;
//    right = r;
//  }
//
//  public Optional<L> left() {
//    return left;
//  }
//
//  public Optional<R> right() {
//    return right;
//  }
//}
