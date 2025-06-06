/**
 * 
 */
package com.github.gtoison.caster.agent;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;

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

			instrumentation.addTransformer(classTransformer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
