package net.crystalnexus.cli;

import java.util.Set;
import java.util.stream.Stream;

final class RecipeCycleGuard {
    private RecipeCycleGuard() {}

    static <T> boolean loopsIntoPath(Stream<T> inputs, T item, Set<T> path) {
        return inputs.anyMatch(input -> input.equals(item) || path.contains(input));
    }
}
