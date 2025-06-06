/**
 * 
 */
package com.github.gtoison.caster.transformer;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Inheritance;

/**
 * @author Guillaume Toison
 */
public class HibernateProxyJandexIndexer {
	private static final Logger LOGGER = LoggerFactory.getLogger(HibernateProxyJandexIndexer.class);

	private Set<String> baseClassesExtensions;

	public HibernateProxyJandexIndexer(Index index) {
		baseClassesExtensions = new HashSet<>();

		for (AnnotationInstance inheritanceInstance : index.getAnnotations(Inheritance.class)) {
			ClassInfo baseClass = inheritanceInstance.target().asClass();
			LOGGER.debug("Indexing base class {}", baseClass);

			addInterfaces(index, baseClass);

			for (ClassInfo subClass : index.getAllKnownSubclasses(baseClass.name())) {
				baseClassesExtensions.add(subClass.toString());
				LOGGER.debug("Indexing subclass {}", subClass);

				addInterfaces(index, subClass);
			}
		}
	}

	private void addInterfaces(Index index, ClassInfo classInfo) {
		for (DotName interfaceName : classInfo.interfaceNames()) {
			baseClassesExtensions.add(interfaceName.toString());

			for (ClassInfo subInterface : index.getAllKnownSubinterfaces(interfaceName)) {
				baseClassesExtensions.add(subInterface.toString());
			}
		}
	}

	public boolean couldBeAProxy(String type) {
		boolean potentialProxy = baseClassesExtensions.contains(type);
		if (potentialProxy) {
			LOGGER.debug("{} could be a proxy", type);
		} else {
			LOGGER.debug("{} cannot be a proxy", type);
		}
		return potentialProxy;
	}

	public boolean couldBeAProxy(ClassEntry type) {
		ClassDesc symbol = type.asSymbol();
		return couldBeAProxy(symbol.packageName() + "." + symbol.displayName());
	}
}
