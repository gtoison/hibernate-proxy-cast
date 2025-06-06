/**
 * 
 */
package com.github.gtoison.caster.transformer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.junit.jupiter.api.Test;

import com.foo.Animal;
import com.foo.Bird;
import com.foo.Insect;
import com.foo.Walker;
import com.foo.Winged;

/**
 * 
 */
class HibernateProxyJandexIndexerTest {

	@Test
	void couldBeAProxyTest() throws IOException {
		try (InputStream input = getClass().getResource("/index.idx").openStream()) {
			IndexReader reader = new IndexReader(input);
			Index index = reader.read();
			
			HibernateProxyJandexIndexer indexer = new HibernateProxyJandexIndexer(index);
			
			assertFalse(indexer.couldBeAProxy(Animal.class.getName()), "Base class can't be a proxy of 'wrong' class");
			assertTrue(indexer.couldBeAProxy(Bird.class.getName()));
			assertTrue(indexer.couldBeAProxy(Insect.class.getName()));
			assertTrue(indexer.couldBeAProxy(Walker.class.getName()));
			assertTrue(indexer.couldBeAProxy(Winged.class.getName()));
		}
	}
}
