/*
 *  Copyright (c) 2017 Otávio Santana and others
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.artemis.document.spi;


import jakarta.nosql.document.DocumentCollectionManager;
import jakarta.nosql.mapping.Repository;
import org.eclipse.jnosql.artemis.DatabaseMetadata;
import org.eclipse.jnosql.artemis.Databases;
import org.eclipse.jnosql.artemis.document.query.RepositoryDocumentBean;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessProducer;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static jakarta.nosql.mapping.DatabaseType.DOCUMENT;

/**
 * Extension to start up the DocumentTemplate and Repository
 * from the {@link jakarta.nosql.mapping.Database} qualifier
 */
public class DocumentCollectionProducerExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(DocumentCollectionProducerExtension.class.getName());

    private final Set<DatabaseMetadata> databases = new HashSet<>();

    private final Collection<Class<?>> crudTypes = new HashSet<>();

    <T extends Repository> void observes(@Observes final ProcessAnnotatedType<T> repo) {
        Class<T> javaClass = repo.getAnnotatedType().getJavaClass();
        if (Repository.class.equals(javaClass)) {
            return;
        }


        if (Arrays.asList(javaClass.getInterfaces()).contains(Repository.class)
                && Modifier.isInterface(javaClass.getModifiers())) {
            LOGGER.info("Adding a new Repository as discovered on document: " + javaClass);
            crudTypes.add(javaClass);
        }
    }


    <T, X extends DocumentCollectionManager> void observes(@Observes final ProcessProducer<T, X> pp) {
        Databases.addDatabase(pp, DOCUMENT, databases);
    }


    void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery afterBeanDiscovery, final BeanManager beanManager) {
        LOGGER.info(String.format("Starting to process on documents: %d databases crud %d ",
                databases.size(), crudTypes.size()));

        databases.forEach(type -> {
            final TemplateBean bean = new TemplateBean(beanManager, type.getProvider());
            afterBeanDiscovery.addBean(bean);
        });


        crudTypes.forEach(type -> {
            if (!databases.contains(DatabaseMetadata.DEFAULT_DOCUMENT)) {
                afterBeanDiscovery.addBean(new RepositoryDocumentBean(type, beanManager, ""));
            }
            databases.forEach(database -> {
                final RepositoryDocumentBean bean = new RepositoryDocumentBean(type, beanManager, database.getProvider());
                afterBeanDiscovery.addBean(bean);
            });
        });

    }

}
