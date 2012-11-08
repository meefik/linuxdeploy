package ru.meefik.linuxdeploy;

import java.util.Map.Entry;

public class Profile<K, V> implements Entry<K, V> {
	private final K key;
	private V value;

	public Profile(final K key) {
		this.key = key;
	}

	public Profile(final K key, final V value) {
		this.key = key;
		this.value = value;
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public V setValue(final V value) {
		final V oldValue = this.value;
		this.value = value;
		return oldValue;
	}

	public String toString() {
		return value.toString();
	}
}
