/**
 * Copyright (c) 2011, University of Konstanz, Distributed Systems Group All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: * Redistributions of source code must retain the
 * above copyright notice, this list of conditions and the following disclaimer. * Redistributions
 * in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * * Neither the name of the University of Konstanz nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.sirix.page;

import com.google.common.base.MoreObjects;
import io.sirix.page.interfaces.Page;
import io.sirix.page.interfaces.PageFragmentKey;
import io.sirix.settings.Constants;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Page reference pointing to a page. This might be on stable storage pointing to the start byte in
 * a file, including the length in bytes, and the checksum of the serialized page. Or it might be an
 * immediate reference to an in-memory instance of the deserialized page.
 * </p>
 */
public final class PageReference {

  /** In-memory deserialized page instance. */
  private volatile Page page;

  /** Key in persistent storage. */
  private volatile long key = Constants.NULL_ID_LONG;

  /** Log key. */
  private volatile int logKey = Constants.NULL_ID_INT;

  /** The hash in bytes, generated from the referenced page-fragment. */
  private volatile byte[] hashInBytes;

  private volatile Int2ObjectRBTreeMap<List<PageFragmentKey>> pageFragments;

  private volatile int hash;

  /**
   * Default constructor setting up an uninitialized page reference.
   */
  public PageReference() {
    pageFragments = new Int2ObjectRBTreeMap<>();
  }

  /**
   * Copy constructor.
   *
   * @param reference {@link PageReference} to copy
   */
  public PageReference(final PageReference reference) {
    logKey = reference.logKey;
    page = reference.page;
    key = reference.key;
    hashInBytes = reference.hashInBytes;
    pageFragments = reference.pageFragments;
    hash = reference.hash;
  }

  /**
   * Set in-memory instance of deserialized page.
   *
   * @param page deserialized page
   */
  public void setPage(final @Nullable Page page) {
    this.page = page;
  }

  /**
   * Get in-memory instance of deserialized page.
   *
   * @return in-memory instance of deserialized page
   */
  public Page getPage() {
    return page;
  }

  /**
   * Get start byte offset in file.
   *
   * @return start offset in file
   */
  public long getKey() {
    return key;
  }

  /**
   * Set start byte offset in file.
   *
   * @param key key of this reference set by the persistent storage
   */
  public PageReference setKey(final long key) {
    hash = 0;
    this.key = key;
    return this;
  }

  /**
   * Add a page fragment key.
   * @param key the page fragment key to add.
   * @return this instance
   */
  public PageReference addPageFragment(final int revision, final PageFragmentKey key) {
    pageFragments.merge(revision, List.of(key), (previous, current) -> {
      var list = new ArrayList<>(previous);
      list.addAll(current);
      return list;
    });
    return this;
  }

  /**
   * Add a page fragment key.
   * @param key the page fragment key to add.
   * @return this instance
   */
  public PageReference setFirstPageFragment(final int revision, final PageFragmentKey key) {
    pageFragments.merge(revision, List.of(key), (previous, current) -> {
      var list = new ArrayList<PageFragmentKey>();
      list.addAll(current);
      for (int i = previous.size() - 1; i > 0; i--) {
        list.add(previous.get(i));
      }
      return list;
    });
    return this;
  }

  /**
   * Get the page fragments keys.
   * @return the page fragments keys
   */
  public List<PageFragmentKey> getPageFragments(final int revision) {
    return pageFragments.getOrDefault(revision, List.of());
  }

  public List<PageFragmentKey> getMostRecentPageFragments() {
    return pageFragments.isEmpty() ? List.of() : pageFragments.get(pageFragments.lastIntKey());
  }

  public Int2ObjectRBTreeMap<List<PageFragmentKey>> getPageFragments() {
    return pageFragments;
  }

  /**
   * Set the page fragment keys.
   * @param previousPageFragmentKeys the previous page fragment keys to set
   * @return this instance
   */
  public PageReference setPageFragments(final Int2ObjectRBTreeMap<List<PageFragmentKey>> previousPageFragmentKeys) {
    pageFragments = previousPageFragmentKeys;
    return this;
  }

  /**
   * Get in-memory log-key.
   *
   * @return log key
   */
  public int getLogKey() {
    return logKey;
  }

  /**
   * Set in-memory log-key.
   *
   * @param key key of this reference set by the transaction intent log.
   * @return this instance
   */
  public PageReference setLogKey(final int key) {
    hash = 0;
    logKey = key;
    return this;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
                      .add("logKey", logKey)
                      .add("key", key)
                      .add("page", page)
                      //.add("pageFragments", pageFragments)
                      .toString();
  }

  @Override
  public int hashCode() {
    if (hash == 0) {
      hash = Objects.hash(logKey, key);
    }
    return hash;
  }

  @Override
  public boolean equals(final @Nullable Object other) {
    if (other instanceof PageReference otherPageRef) {
      return otherPageRef.logKey == logKey && otherPageRef.key == key;
    }
    return false;
  }

  public void setHash(byte[] hashInBytes) {
    this.hashInBytes = hashInBytes;
  }

  public byte[] getHash() {
    return hashInBytes;
  }
}
