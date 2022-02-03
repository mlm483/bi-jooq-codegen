/*
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.breedinginsight.generation;

import lombok.extern.slf4j.Slf4j;
import org.jooq.Configuration;
import org.jooq.Constants;
import org.jooq.Record;
import org.jooq.codegen.GeneratorStrategy.Mode;
import org.jooq.codegen.GeneratorWriter;
import org.jooq.codegen.JavaGenerator;
import org.jooq.codegen.JavaWriter;
import org.jooq.impl.DAOImpl;
import org.jooq.meta.*;
import org.jooq.tools.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class JooqDaoGenerator extends JavaGenerator {

    private boolean scala = false;

    @Override
    protected void printClassAnnotations(JavaWriter out, Definition definition, Mode mode) {
        if (mode.equals(mode.DAO)) {
            // Add singleton annotation
            super.printClassAnnotations(out, definition, mode);
            out.println("@%s", out.ref("javax.inject.Singleton"));
        } else if (mode.equals(mode.POJO)) {
            // Add SuperBuilder
            super.printClassAnnotations(out, definition, mode);
            out.tab(1).println("@%s", out.ref("lombok.experimental.SuperBuilder"));
        }
    }

    @Override
    protected void printDaoConstructorAnnotations(TableDefinition table, JavaWriter out) {
        super.printDaoConstructorAnnotations(table, out);
        out.println("@%s", out.ref("javax.inject.Inject"));
    }

}
