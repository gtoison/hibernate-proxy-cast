/**
 * 
 */
package com.github.gtoison.caster.transformer;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFile.ClassHierarchyResolverOption;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.MethodTransform;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.gtoison.caster.HibernateProxyCaster;

/**
 * @author Guillaume Toison
 */
public class HibernateProxyCasterClassTransformer implements ClassFileTransformer {
	private static final Logger LOGGER = LoggerFactory.getLogger(HibernateProxyCasterClassTransformer.class);

	private static final ClassDesc CD_BOOLEAN_TYPE = ClassDesc.ofDescriptor("Z");
	private static final ClassDesc CD_OBJECT = ClassDesc.of("java.lang.Object");
	private static final ClassDesc CD_CLASS = ClassDesc.of("java.lang.Class");
	private static final ClassDesc CD_HIBERNATE_CASTER = ClassDesc.of(HibernateProxyCaster.class.getName());

	private HibernateProxyJandexIndexer indexer;

	public HibernateProxyCasterClassTransformer(HibernateProxyJandexIndexer indexer) {
		this.indexer = indexer;
	}

	@Override
	public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		try {
			return transform(loader, className, classfileBuffer);
		} catch (Exception e) {
			throw new ClassTransformationException("Error transforming class " + className, e);
		}
	}

	public byte[] transform(ClassLoader loader, String className, byte[] classfileBuffer) throws IllegalClassFormatException {
		if (loader == null) {
			// For base Java classes the loader is null and we don't want to transform
			return null;
		}
		
		try {
			loader.loadClass("org.hibernate.Hibernate");
		} catch (ClassNotFoundException e) {
			throw new ClassTransformationException("Hibernate not available, cannot transform " + className, e);
		}

		if (className.startsWith("org/hibernate/")) {
			// We do not want to transform hibernate classes, in particular org.hibernate.Hibernate because it would call itself in a loop
			return null;
		}

		LOGGER.debug("Transforming {}", className);

		ClassHierarchyResolver classHierarchyResolver = ClassHierarchyResolver.ofResourceParsing(loader);
		ClassHierarchyResolverOption classHierarchyResolverOption = ClassHierarchyResolverOption.of(classHierarchyResolver);
		ClassFile classFile = ClassFile.of(classHierarchyResolverOption);
		ClassModel classModel = classFile.parse(classfileBuffer);

		CodeTransform codeTransform = (codeBuilder, codeElement) -> {
			switch (codeElement) {
				case TypeCheckInstruction i when i.opcode() == Opcode.INSTANCEOF &&  indexer.couldBeAProxy(i.type()) -> {
					codeBuilder.ldc(i.type());
					codeBuilder.invokestatic(CD_HIBERNATE_CASTER, "proxyInstanceof", MethodTypeDesc.of(CD_BOOLEAN_TYPE, CD_OBJECT, CD_CLASS));
				}
				case TypeCheckInstruction i when i.opcode() == Opcode.CHECKCAST &&  indexer.couldBeAProxy(i.type()) -> {
					codeBuilder.ldc(i.type());
					codeBuilder.invokestatic(CD_HIBERNATE_CASTER, "proxyCast", MethodTypeDesc.of(CD_OBJECT, CD_OBJECT, CD_CLASS));
					codeBuilder.checkcast(i.type());
				}
				default -> codeBuilder.accept(codeElement);
			}
		};

		MethodTransform methodTransform = MethodTransform.transformingCode(codeTransform);
		ClassTransform classTransform = ClassTransform.transformingMethods(methodTransform);

		return classFile.transformClass(classModel, classTransform);
	}
}
