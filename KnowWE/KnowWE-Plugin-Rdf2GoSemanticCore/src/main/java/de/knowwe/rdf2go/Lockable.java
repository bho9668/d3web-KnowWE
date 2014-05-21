package de.knowwe.rdf2go;

/**
 * Interface allowing to perform a lock operation.
 *
 * Created by Albrecht Striffler (denkbares GmbH) on 11.05.2014.
 */
public interface Lockable {

	void lock();

	void unlock();
}