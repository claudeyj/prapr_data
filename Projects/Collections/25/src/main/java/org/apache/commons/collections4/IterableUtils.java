/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections4;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.functors.EqualPredicate;
import org.apache.commons.collections4.iterators.LazyIteratorChain;
import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.commons.collections4.iterators.UniqueFilterIterator;
import org.apache.commons.collections4.iterators.ZippingIterator;

/**
 * Provides utility methods and decorators for {@link Iterable} instances.
 * <p>
 * <b>Note</b>: by design, all provided utility methods will treat a {@code null}
 * {@link Iterable} parameters the same way as an empty iterable. All other required
 * parameters which are null, e.g. a {@link Predicate}, will result in a
 * {@link NullPointerException}.
 *
 * @since 4.1
 * @version $Id$
 */
public class IterableUtils {

    // Chained
    // ----------------------------------------------------------------------

    /**
     * Combines two iterables into a single iterable.
     * <p>
     * The returned iterable has an iterator that traverses the elements in {@code a},
     * followed by the elements in {@code b}. The source iterators are not polled until
     * necessary.
     * <p>
     * The returned iterable's iterator supports {@code remove()} when the corresponding
     * input iterator supports it.
     *
     * @param <E>  the element type
     * @param a  the first iterable
     * @param b  the second iterable
     * @return a new iterable, combining the provided iterables
     */
    @SuppressWarnings("unchecked")
    public static <E> Iterable<E> chainedIterable(final Iterable<? extends E> a,
                                                  final Iterable<? extends E> b) {
        return chainedIterable(new Iterable[] {a, b});
    }

    /**
     * Combines three iterables into a single iterable.
     * <p>
     * The returned iterable has an iterator that traverses the elements in {@code a},
     * followed by the elements in {@code b} and {@code c}. The source iterators are
     * not polled until necessary.
     * <p>
     * The returned iterable's iterator supports {@code remove()} when the corresponding
     * input iterator supports it.
     *
     * @param <E>  the element type
     * @param a  the first iterable
     * @param b  the second iterable
     * @param c  the third iterable
     * @return a new iterable, combining the provided iterables
     */
    @SuppressWarnings("unchecked")
    public static <E> Iterable<E> chainedIterable(final Iterable<? extends E> a,
                                                  final Iterable<? extends E> b,
                                                  final Iterable<? extends E> c) {
        return chainedIterable(new Iterable[] {a, b, c});
    }

    /**
     * Combines four iterables into a single iterable.
     * <p>
     * The returned iterable has an iterator that traverses the elements in {@code a},
     * followed by the elements in {@code b}, {@code c} and {@code d}. The source
     * iterators are not polled until necessary.
     * <p>
     * The returned iterable's iterator supports {@code remove()} when the corresponding
     * input iterator supports it.
     *
     * @param <E>  the element type
     * @param a  the first iterable
     * @param b  the second iterable
     * @param c  the third iterable
     * @param d  the fourth iterable
     * @return a new iterable, combining the provided iterables
     */
    @SuppressWarnings("unchecked")
    public static <E> Iterable<E> chainedIterable(final Iterable<? extends E> a,
                                                  final Iterable<? extends E> b,
                                                  final Iterable<? extends E> c,
                                                  final Iterable<? extends E> d) {
        return chainedIterable(new Iterable[] {a, b, c, d});
    }

    /**
     * Combines the provided iterables into a single iterable.
     * <p>
     * The returned iterable has an iterator that traverses the elements in the order
     * of the arguments, i.e. iterables[0], iterables[1], .... The source iterators
     * are not polled until necessary.
     * <p>
     * The returned iterable's iterator supports {@code remove()} when the corresponding
     * input iterator supports it.
     *
     * @param <E>  the element type
     * @param iterables  the iterables to combine
     * @return a new iterable, combining the provided iterables
     */
    public static <E> Iterable<E> chainedIterable(final Iterable<? extends E>... iterables) {
        return new FluentIterable<E>() {
            @Override
            public Iterator<E> iterator() {
                return new LazyIteratorChain<E>() {
                    @Override
                    protected Iterator<? extends E> nextIterator(int count) {
                        if (count > iterables.length) {
                            return null;
                        } else {
                            return emptyIteratorIfNull(iterables[count - 1]);
                        }
                    }
                };
            }
        };
    }

    // Collated
    // ----------------------------------------------------------------------

    /**
     * Combines the two provided iterables into an ordered iterable using the
     * provided comparator. If the comparator is null, natural ordering will be
     * used.
     * <p>
     * The returned iterable's iterator supports {@code remove()} when the corresponding
     * input iterator supports it.
     *
     * @param <E>  the element type
     * @param a  the first iterable, may be null
     * @param b  the second iterable, may be null
     * @param comparator  the comparator defining an ordering over the elements,
     *   may be null, in which case natural ordering will be used
     * @return a filtered view on the specified iterable
     */
    public static <E> Iterable<E> collatedIterable(final Iterable<? extends E> a,
                                                   final Iterable<? extends E> b,
                                                   final Comparator<? super E> comparator) {
        return new FluentIterable<E>() {
            @Override
            public Iterator<E> iterator() {
                return IteratorUtils.collatedIterator(comparator,
                                                      emptyIteratorIfNull(a),
                                                      emptyIteratorIfNull(b));
            }
        };
    }

    // Filtered
    // ----------------------------------------------------------------------

    /**
     * Returns a view of the given iterable that only contains elements matching
     * the provided predicate.
     * <p>
     * The returned iterable's iterator does not supports {@code remove()}.
     *
     * @param <E>  the element type
     * @param iterable  the iterable to filter, may be null
     * @param predicate  the predicate used to filter elements, must not be null
     * @return a filtered view on the specified iterable
     * @throws NullPointerException if predicate is null
     */
    public static <E> Iterable<E> filteredIterable(final Iterable<E> iterable,
                                                   final Predicate<? super E> predicate) {
        if (predicate == null) {
            throw new NullPointerException("predicate must not be null.");
        }

        return new FluentIterable<E>() {
            @Override
            public Iterator<E> iterator() {
                return IteratorUtils.filteredIterator(emptyIteratorIfNull(iterable), predicate);
            }
        };
    }

    // Bounded
    // ----------------------------------------------------------------------

    /**
     * Returns a view of the given iterable that contains at most the given number
     * of elements.
     * <p>
     * The returned iterable's iterator supports {@code remove()} when the corresponding
     * input iterator supports it.
     *
     * @param <E>  the element type
     * @param iterable  the iterable to limit, may be null
     * @param maxSize  the maximum number of elements, must not be negative
     * @return a bounded view on the specified iterable
     * @throws IllegalArgumentException if maxSize is negative
     */
    public static <E> Iterable<E> boundedIterable(final Iterable<E> iterable, final long maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize parameter must not be negative.");
        }

        return new FluentIterable<E>() {
            @Override
            public Iterator<E> iterator() {
                return IteratorUtils.boundedIterator(emptyIteratorIfNull(iterable), maxSize);
            }
        };
    }

    // Looping
    // ----------------------------------------------------------------------

    /**
     * Returns a view of the given iterable which will cycle infinitely over
     * its elements.
     * <p>
     * The returned iterable's iterator supports {@code remove()} if
     * {@code iterable.iterator()} does. After {@code remove()} is called, subsequent
     * cycles omit the removed element, which is no longer in {@code iterable}. The
     * iterator's {@code hasNext()} method returns {@code true} until {@code iterable}
     * is empty.
     *
     * @param <E>  the element type
     * @param iterable  the iterable to loop, may be null
     * @return a view of the iterable, providing an infinite loop over its elements
     */
    public static <E> Iterable<E> loopingIterable(final Iterable<E> iterable) {
        return new FluentIterable<E>() {
            @Override
            public Iterator<E> iterator() {
                return new LazyIteratorChain<E>() {
                    @Override
                    protected Iterator<? extends E> nextIterator(int count) {
                        if (iterable != null) {
                            if (IterableUtils.isEmpty(iterable)) {
                                return null;
                            } else {
                                return iterable.iterator();
                            }
                        } else {
                            return null;
                        }
                    }
                };
            }
        };
    }

    // Reversed
    // ----------------------------------------------------------------------

    /**
     * Returns a reversed view of the given iterable.
     * <p>
     * In case the provided iterable is a {@link List} instance, a
     * {@link ReverseListIterator} will be used to reverse the traversal
     * order, otherwise an intermediate {@link List} needs to be created.
     * <p>
     * The returned iterable's iterator supports {@code remove()} if the
     * provided iterable is a {@link List} instance.
     *
     * @param <E>  the element type
     * @param iterable  the iterable to use, may be null
     * @return a reversed view of the specified iterable
     * @see ReverseListIterator
     */
    public static <E> Iterable<E> reversedIterable(final Iterable<E> iterable) {
        return new FluentIterable<E>() {
            @Override
            public Iterator<E> iterator() {
                final List<E> list = (iterable instanceof List<?>) ?
                        (List<E>) iterable :
                        IteratorUtils.toList(emptyIteratorIfNull(iterable));

                return new ReverseListIterator<E>(list);
            }
        };
    }

    // Skipping
    // ----------------------------------------------------------------------

    /**
     * Returns a view of the given iterable that skips the first N elements.
     * <p>
     * The returned iterable's iterator supports {@code remove()} when the corresponding
     * input iterator supports it.
     *
     * @param <E>  the element type
     * @param iterable  the iterable to use, may be null
     * @param elementsToSkip  the number of elements to skip from the start, must not be negative
     * @return a view of the specified iterable, skipping the first N elements
     * @throws IllegalArgumentException if elementsToSkip is negative
     */
    public static <E> Iterable<E> skippingIterable(final Iterable<E> iterable, final long elementsToSkip) {
        if (elementsToSkip < 0) {
            throw new IllegalArgumentException("elementsToSkip parameter must not be negative.");
        }

        return new FluentIterable<E>() {
            @Override
            public Iterator<E> iterator() {
                return IteratorUtils.skippingIterator(emptyIteratorIfNull(iterable), elementsToSkip);
            }
        };
    }

    // Transformed
    // ----------------------------------------------------------------------

    /**
     * Returns a transformed view of the given iterable where all of its elements
     * have been transformed by the provided transformer.
     * <p>
     * The returned iterable's iterator supports {@code remove()} when the corresponding
     * input iterator supports it.
     *
     * @param <I>  the input element type
     * @param <O>  the output element type
     * @param iterable  the iterable to transform, may be null
     * @param transformer  the transformer , must not be null
     * @return a transformed view of the specified iterable
     * @throws NullPointerException if transformer is null
     */
    public static <I, O> Iterable<O> transformedIterable(final Iterable<I> iterable,
                                                         final Transformer<? super I, ? extends O> transformer) {
        if (transformer == null) {
            throw new NullPointerException("transformer must not be null.");
        }

        return new FluentIterable<O>() {
            @Override
            public Iterator<O> iterator() {
                return IteratorUtils.transformedIterator(emptyIteratorIfNull(iterable), transformer);
            }
        };
    }

    // Unique
    // ----------------------------------------------------------------------

    /**
     * Returns a unique view of the given iterable.
     * <p>
     * The returned iterable's iterator does not supports {@code remove()}.
     *
     * @param <E>  the element type
     * @param iterable  the iterable to transform, may be null
     * @return a unique view of the specified iterable
     */
    public static <E> Iterable<E> uniqueIterable(final Iterable<E> iterable) {
        return new FluentIterable<E>() {
            @Override
            public Iterator<E> iterator() {
                return new UniqueFilterIterator<E>(emptyIteratorIfNull(iterable));
            }
        };
    }

    // Zipping
    // ----------------------------------------------------------------------

    /**
     * Interleaves two iterables into a single iterable.
     * <p>
     * The returned iterable has an iterator that traverses the elements in {@code a}
     * and {@code b} in alternating order. The source iterators are not polled until
     * necessary.
     * <p>
     * The returned iterable's iterator supports {@code remove()} when the corresponding
     * input iterator supports it.
     *
     * @param <E>  the element type
     * @param a  the first iterable
     * @param b  the second iterable
     * @return a new iterable, interleaving the provided iterables
     */
    @SuppressWarnings("unchecked")
    public static <E> Iterable<E> zippingIterable(final Iterable<? extends E> a,
                                                  final Iterable<? extends E> b) {
        return zippingIterable(new Iterable[] {a, b});
    }

    /**
     * Interleaves two iterables into a single iterable.
     * <p>
     * The returned iterable has an iterator that traverses the elements in {@code a}
     * and {@code b} in alternating order. The source iterators are not polled until
     * necessary.
     * <p>
     * The returned iterable's iterator supports {@code remove()} when the corresponding
     * input iterator supports it.
     *
     * @param <E>  the element type
     * @param iterables  the array of iterables to interleave
     * @return a new iterable, interleaving the provided iterables
     */
    public static <E> Iterable<E> zippingIterable(final Iterable<? extends E>... iterables) {
        return new FluentIterable<E>() {
            @Override
            public Iterator<E> iterator() {
                @SuppressWarnings("unchecked")
                Iterator<? extends E>[] iterators = new Iterator[iterables.length];
                for (int i = 0; i < iterables.length; i++) {
                    iterators[i] = emptyIteratorIfNull(iterables[i]);
                }
                return new ZippingIterator<E>(iterators);
            }
        };
    }

    // Utility methods
    // ----------------------------------------------------------------------

    /**
     * Returns an empty iterator if the argument is <code>null</code>,
     * or returns {@code iterable.iterator()} otherwise.
     *
     * @param <E> the element type
     * @param iterable  the iterable, possibly <code>null</code>
     * @return an empty collection if the argument is <code>null</code>
     */
    public static <E> Iterator<E> emptyIteratorIfNull(final Iterable<E> iterable) {
        return iterable != null ? iterable.iterator() : IteratorUtils.<E>emptyIterator();
    }

    /**
     * Applies the closure to each element of the provided iterable.
     *
     * @param <E>  the element type
     * @param iterable  the iterator to use, may be null
     * @param closure  the closure to apply to each element, may not be null
     * @throws NullPointerException if closure is null
     */
    public static <E> void apply(final Iterable<E> iterable, final Closure<? super E> closure) {
        IteratorUtils.apply(emptyIteratorIfNull(iterable), closure);
    }

    /**
     * Finds the first element in the given iterable which matches the given predicate.
     * <p>
     * A <code>null</code> or empty iterator returns null.
     *
     * @param <E>  the element type
     * @param iterable  the iterable to search, may be null
     * @param predicate  the predicate to use, may not be null
     * @return the first element of the iterable which matches the predicate or null if none could be found
     * @throws NullPointerException if predicate is null
     */
    public static <E> E find(final Iterable<E> iterable, final Predicate<? super E> predicate) {
        return IteratorUtils.find(emptyIteratorIfNull(iterable), predicate);
    }

    /**
     * Answers true if a predicate is true for every element of an iterable.
     * <p>
     * A <code>null</code> or empty iterable returns true.
     *
     * @param <E>  the type of object the {@link Iterable} contains
     * @param iterable  the {@link Iterable} to use, may be null
     * @param predicate  the predicate to use, may not be null
     * @return true if every element of the collection matches the predicate or if the
     *   collection is empty, false otherwise
     * @throws NullPointerException if predicate is null
     */
    public static <E> boolean matchesAll(final Iterable<E> iterable, final Predicate<? super E> predicate) {
        return IteratorUtils.matchesAll(emptyIteratorIfNull(iterable), predicate);
    }

    /**
     * Answers true if a predicate is true for any element of the iterable.
     * <p>
     * A <code>null</code> or empty iterable returns false.
     *
     * @param <E>  the type of object the {@link Iterable} contains
     * @param iterable  the {@link Iterable} to use, may be null
     * @param predicate  the predicate to use, may not be null
     * @return true if any element of the collection matches the predicate, false otherwise
     * @throws NullPointerException if predicate is null
     */
    public static <E> boolean matchesAny(final Iterable<E> iterable, final Predicate<? super E> predicate) {
        return IteratorUtils.matchesAny(emptyIteratorIfNull(iterable), predicate);
    }

    /**
     * Counts the number of elements in the input iterable that match the predicate.
     * <p>
     * A <code>null</code> iterable matches no elements.
     *
     * @param <E>  the type of object the {@link Iterable} contains
     * @param input  the {@link Iterable} to get the input from, may be null
     * @param predicate  the predicate to use, may not be null
     * @return the number of matches for the predicate in the collection
     * @throws NullPointerException if predicate is null
     */
    public static <E> long frequency(final Iterable<E> input, final Predicate<? super E> predicate) {
        if (predicate == null) {
            throw new NullPointerException("Predicate must not be null.");
        }
        long count = 0;
        if (input != null) {
            for (final E o : input) {
                if (predicate.evaluate(o)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Answers true if the provided iterable is empty.
     * <p>
     * A <code>null</code> iterable returns true.
     *
     * @param iterable  the {@link Iterable to use}, may be null
     * @return true if the iterable is null or empty, false otherwise
     */
    public static boolean isEmpty(final Iterable<?> iterable) {
        if (iterable instanceof Collection<?>) {
            return ((Collection<?>) iterable).isEmpty();
        } else {
            return IteratorUtils.isEmpty(emptyIteratorIfNull(iterable));
        }
    }

    /**
     * Checks if the object is contained in the given iterable.
     * <p>
     * A <code>null</code> or empty iterable returns false.
     *
     * @param <E>  the type of object the {@link Iterable} contains
     * @param iterable  the iterable to check, may be null
     * @param object  the object to check
     * @return true if the object is contained in the iterable, false otherwise
     */
    public static <E> boolean contains(final Iterable<E> iterable, final Object object) {
        if (iterable instanceof Collection<?>) {
            return ((Collection<E>) iterable).contains(object);
        } else {
            return IteratorUtils.contains(emptyIteratorIfNull(iterable), object);
        }
    }

    /**
     * Checks if the object is contained in the given iterable. Object equality
     * is tested with an {@code equator} unlike {@link #contains(Iterable, Object)}
     * which uses {@link Object#equals(Object)}.
     * <p>
     * A <code>null</code> or empty iterable returns false.
     * A <code>null</code> object will not be passed to the equator, instead a
     * {@link org.apache.commons.collections4.functors.NullPredicate NullPredicate}
     * will be used.
     *
     * @param <E>  the type of object the {@link Iterable} contains
     * @param iterable  the iterable to check, may be null
     * @param object  the object to check
     * @param equator  the equator to use to check, may not be null
     * @return true if the object is contained in the iterable, false otherwise
     * @throws NullPointerException if equator is null
     */
    public static <E> boolean contains(final Iterable<? extends E> iterable, final E object,
                                       final Equator<? super E> equator) {
        if (equator == null) {
            throw new NullPointerException("Equator must not be null.");
        }
        return matchesAny(iterable, EqualPredicate.equalPredicate(object, equator));
    }

    /**
     * Returns the <code>index</code>-th value in the <code>iterable</code>'s {@link Iterator}, throwing
     * <code>IndexOutOfBoundsException</code> if there is no such element.
     * <p>
     * If the {@link Iterable} is a {@link List}, then it will use {@link List#get(int)}.
     *
     * @param <T> the type of object in the {@link Iterable}.
     * @param iterable  the {@link Iterable} to get a value from, may be null
     * @param index  the index to get
     * @return the object at the specified index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public static <T> T get(final Iterable<T> iterable, final int index) {
        CollectionUtils.checkIndexBounds(index);
        if (iterable instanceof List<?>) {
            return ((List<T>) iterable).get(index);
        }
        return IteratorUtils.get(emptyIteratorIfNull(iterable), index);
    }

    /**
     * Returns the number of elements contained in the given iterator.
     * <p>
     * A <code>null</code> or empty iterator returns {@code 0}.
     *
     * @param iterable  the iterable to check, may be null
     * @return the number of elements contained in the iterable
     */
    public static int size(final Iterable<?> iterable) {
        if (iterable instanceof Collection<?>) {
            return ((Collection<?>) iterable).size();
        } else {
            return IteratorUtils.size(emptyIteratorIfNull(iterable));
        }
    }

    /**
     * Returns a string representation of the elements of the specified iterable.
     * <p>
     * The string representation consists of a list of the iterable's elements,
     * enclosed in square brackets ({@code "[]"}). Adjacent elements are separated
     * by the characters {@code ", "} (a comma followed by a space). Elements are
     * converted to strings as by {@code String.valueOf(Object)}.
     *
     * @param <E>  the element type
     * @param iterable  the iterable to convert to a string, may be null
     * @return a string representation of {@code iterable}
     */
    public static <E> String toString(final Iterable<E> iterable) {
        return IteratorUtils.toString(emptyIteratorIfNull(iterable));
    }

    /**
     * Returns a string representation of the elements of the specified iterable.
     * <p>
     * The string representation consists of a list of the iterable's elements,
     * enclosed in square brackets ({@code "[]"}). Adjacent elements are separated
     * by the characters {@code ", "} (a comma followed by a space). Elements are
     * converted to strings as by using the provided {@code transformer}.
     *
     * @param <E>  the element type
     * @param iterable  the iterable to convert to a string, may be null
     * @param transformer  the transformer used to get a string representation of an element
     * @return a string representation of {@code iterable}
     * @throws NullPointerException if {@code transformer} is null
     */
    public static <E> String toString(final Iterable<E> iterable,
                                      final Transformer<? super E, String> transformer) {
        if (transformer == null) {
            throw new NullPointerException("transformer may not be null");
        }
        return IteratorUtils.toString(emptyIteratorIfNull(iterable), transformer);
    }

    /**
     * Returns a string representation of the elements of the specified iterable.
     * <p>
     * The string representation consists of a list of the iterable's elements,
     * enclosed by the provided {@code prefix} and {@code suffix}. Adjacent elements
     * are separated by the provided {@code delimiter}. Elements are converted to
     * strings as by using the provided {@code transformer}.
     *
     * @param <E>  the element type
     * @param iterable  the iterable to convert to a string, may be null
     * @param transformer  the transformer used to get a string representation of an element
     * @param delimiter  the string to delimit elements
     * @param prefix  the prefix, prepended to the string representation
     * @param suffix  the suffix, appended to the string representation
     * @return a string representation of {@code iterable}
     * @throws NullPointerException if either transformer, delimiter, prefix or suffix is null
     */
    public static <E> String toString(final Iterable<E> iterable,
                                      final Transformer<? super E, String> transformer,
                                      final String delimiter,
                                      final String prefix,
                                      final String suffix) {
        return IteratorUtils.toString(emptyIteratorIfNull(iterable),
                                      transformer, delimiter, prefix, suffix);
    }

}
