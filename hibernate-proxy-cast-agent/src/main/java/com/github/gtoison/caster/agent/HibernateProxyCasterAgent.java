/**
 * 
 */
package com.github.gtoison.caster.agent;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;

import com.github.gtoison.caster.transformer.HibernateProxyCasterClassTransformer;
import com.github.gtoison.caster.transformer.HibernateProxyJandexIndexer;

/**
 * @author Guillaume Toison
 */
public final class HibernateProxyCasterAgent {
	public static void premain(String agentArgs, Instrumentation instrumentation) {
		try (InputStream input = HibernateProxyCasterClassTransformer.class.getResource("/META-INF/jandex.idx").openStream()) {
			IndexReader reader = new IndexReader(input);
			Index index = reader.read();

			HibernateProxyJandexIndexer indexer = new HibernateProxyJandexIndexer(index);
			HibernateProxyCasterClassTransformer classTransformer = new HibernateProxyCasterClassTransformer(indexer);

			instrumentation.addTransformer(classTransformer, true);
			
			// Initializing the class transformer might trigger the load of some user classes, for instance when slf4j loads some user defined log4j plugins
			List<Class<?>> transformableLoadedClasses = new ArrayList<>();
			for (Class<?> loadedClass : instrumentation.getAllLoadedClasses()) {
				if (instrumentation.isModifiableClass(loadedClass)) {
					transformableLoadedClasses.add(loadedClass);
					
					try {
						instrumentation.retransformClasses(loadedClass);
					} catch (Throwable t) {
						System.err.println("Error transforming " + loadedClass + ": " + t.getMessage());
						t.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
