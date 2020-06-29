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
    protected void generateDao(TableDefinition table, JavaWriter out) {
        try {
            this.generateInjectableDao(table, out);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void generatePojo(TableDefinition table) {
        JavaWriter out = this.newJavaWriter(this.getFile(table, Mode.POJO));
        log.info("Generating POJO", out.file().getName());
        this.generatePojo(table, out);
        this.closeJavaWriter(out);
    }

    @Override
    protected void generateUDTPojo(UDTDefinition udt) {
        JavaWriter out = this.newJavaWriter(this.getFile(udt, Mode.POJO));
        log.info("Generating POJO", out.file().getName());
        try {
            this.generateBuildablePojo(udt, out);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        } finally {
            this.closeJavaWriter(out);
        }
    }

    @Override
    protected void generatePojo(TableDefinition table, JavaWriter out) {
        try {
            this.generateBuildablePojo(table, out);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void generateUDTPojo(UDTDefinition udt, JavaWriter out) {
        try {
            this.generateBuildablePojo(udt, out);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private final void generateBuildablePojo(Definition tableOrUDT, JavaWriter out) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String className = this.getStrategy().getJavaClassName(tableOrUDT, Mode.POJO);
        String interfaceName = this.generateInterfaces() ? out.ref(this.getStrategy().getFullJavaClassName(tableOrUDT, Mode.INTERFACE)) : "";
        String superName = out.ref(this.getStrategy().getJavaClassExtends(tableOrUDT, Mode.POJO));
        List<String> interfaces = out.ref(this.getStrategy().getJavaClassImplements(tableOrUDT, Mode.POJO));
        List<String> superTypes = list(superName, interfaces);
        if (this.generateInterfaces()) {
            interfaces.add(interfaceName);
        }

        this.printPackage(out, tableOrUDT, Mode.POJO);
        if (tableOrUDT instanceof TableDefinition) {
            this.generatePojoClassJavadoc((TableDefinition)tableOrUDT, out);
        } else {
            this.generateUDTPojoClassJavadoc((UDTDefinition)tableOrUDT, out);
        }

        this.printClassAnnotations(out, tableOrUDT.getSchema());
        if (tableOrUDT instanceof TableDefinition) {
            this.printTableJPAAnnotation(out, (TableDefinition)tableOrUDT);
        }

        int maxLength = 0;

        Iterator var9;
        TypedElementDefinition column;
        for(var9 = this.getTypedElements(tableOrUDT).iterator(); var9.hasNext(); maxLength = Math.max(maxLength, out.ref(this.getJavaType(column.getType(this.resolver(Mode.POJO)), Mode.POJO)).length())) {
            column = (TypedElementDefinition)var9.next();
        }

//        TypedElementDefinition column;
        if (this.scala) {
            out.println("%sclass %s(", new Object[]{this.generateImmutablePojos() ? "case " : "", className});
            String separator = "  ";

            for(Iterator var13 = this.getTypedElements(tableOrUDT).iterator(); var13.hasNext(); separator = ", ") {
                column = (TypedElementDefinition)var13.next();
                ((JavaWriter)out.tab(1)).println("%s%s%s : %s", new Object[]{separator, this.generateImmutablePojos() ? "" : "private var ", this.getStrategy().getJavaMemberName(column, Mode.POJO), out.ref(this.getJavaType(column.getType(this.resolver(Mode.POJO)), Mode.POJO))});
            }

            out.println(")[[before= extends ][%s]][[before= with ][separator= with ][%s]] {", new Object[]{first(superTypes), remaining(superTypes)});
        } else {
            out.tab(1).println("@%s", out.ref("lombok.experimental.SuperBuilder"));
            out.println("public class %s[[before= extends ][%s]][[before= implements ][%s]] {", new Object[]{className, list(superName), interfaces});
            if (this.generateSerializablePojos() || this.generateSerializableInterfaces()) {
                out.printSerial();
            }

            out.println();
            var9 = this.getTypedElements(tableOrUDT).iterator();

            while(var9.hasNext()) {
                column = (TypedElementDefinition)var9.next();
                ((JavaWriter)out.tab(1)).println("private %s%s %s;", new Object[]{this.generateImmutablePojos() ? "final " : "", StringUtils.rightPad(out.ref(this.getJavaType(column.getType(this.resolver(Mode.POJO)), Mode.POJO)), maxLength), this.getStrategy().getJavaMemberName(column, Mode.POJO)});
            }
        }

        if (!this.generateImmutablePojos()) {
            this.generatePojoDefaultConstructor(tableOrUDT, out);
        }

        this.generatePojoCopyConstructor(tableOrUDT, out);
        this.generatePojoMultiConstructor(tableOrUDT, out);
        List<? extends TypedElementDefinition<?>> elements = this.getTypedElements(tableOrUDT);

        for(int i = 0; i < elements.size(); ++i) {
            column = (TypedElementDefinition)elements.get(i);
            if (tableOrUDT instanceof TableDefinition) {
                this.generatePojoGetter(column, i, out);
            } else {
                this.generateUDTPojoGetter(column, i, out);
            }

            if (!this.generateImmutablePojos()) {
                if (tableOrUDT instanceof TableDefinition) {
                    this.generatePojoSetter(column, i, out);
                } else {
                    this.generateUDTPojoSetter(column, i, out);
                }
            }
        }

        if (this.generatePojosEqualsAndHashCode()) {
            this.generatePojoEqualsAndHashCode(tableOrUDT, out);
        }

        if (this.generatePojosToString()) {
            this.generatePojoToString(tableOrUDT, out);
        }

        if (this.generateInterfaces() && !this.generateImmutablePojos()) {
            this.printFromAndInto(out, tableOrUDT);
        }

        if (tableOrUDT instanceof TableDefinition) {
            this.generatePojoClassFooter((TableDefinition)tableOrUDT, out);
        } else {
            this.generateUDTPojoClassFooter((UDTDefinition)tableOrUDT, out);
        }

        out.println("}");
        this.closeJavaWriter(out);
    }

    private void generateInjectableDao(TableDefinition table, JavaWriter out) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        UniqueKeyDefinition key = table.getPrimaryKey();
        if (key == null) {
            log.info("Skipping DAO generation", out.file().getName());
            return;
        }

        final String className = getStrategy().getJavaClassName(table, Mode.DAO);
        final List<String> interfaces = out.ref(getStrategy().getJavaClassImplements(table, Mode.DAO));
        final String tableRecord = out.ref(getStrategy().getFullJavaClassName(table, Mode.RECORD));
        final String daoImpl = out.ref(DAOImpl.class);
        final String tableIdentifier = executeJavaWriterRef(out, getStrategy().getFullJavaIdentifier(table), 2);//out.ref(getStrategy().getFullJavaIdentifier(table), 2);

        String tType = (scala ? "Unit" : "Void");
        String pType = out.ref(getStrategy().getFullJavaClassName(table, Mode.POJO));

        List<ColumnDefinition> keyColumns = key.getKeyColumns();

        if (keyColumns.size() == 1) {
            tType = getJavaType(keyColumns.get(0).getType(resolver()), Mode.POJO);
        }
        else if (keyColumns.size() <= Constants.MAX_ROW_DEGREE) {
            String generics = "";
            String separator = "";

            for (ColumnDefinition column : keyColumns) {
                generics += separator + out.ref(getJavaType(column.getType(resolver())));
                separator = ", ";
            }

            if (scala)
                tType = Record.class.getName() + keyColumns.size() + "[" + generics + "]";
            else
                tType = Record.class.getName() + keyColumns.size() + "<" + generics + ">";
        }
        else {
            tType = Record.class.getName();
        }

        tType = out.ref(tType);

        printPackage(out, table, Mode.DAO);
        generateDaoClassJavadoc(table, out);
        printClassAnnotations(out, table.getSchema());

        if (generateSpringAnnotations())
            out.println("@%s", out.ref("org.springframework.stereotype.Repository"));

        if (scala)
            out.println("class %s(configuration : %s) extends %s[%s, %s, %s](%s, classOf[%s], configuration)[[before= with ][separator= with ][%s]] {",
                        className, Configuration.class, daoImpl, tableRecord, pType, tType, tableIdentifier, pType, interfaces);
        else {
            out.println("@%s", out.ref("javax.inject.Singleton"));
            out.println("public class %s extends %s<%s, %s, %s>[[before= implements ][%s]] {", className, daoImpl, tableRecord, pType, tType, interfaces);
        }

        // Default constructor
        // -------------------
        out.tab(1).javadoc("Create a new %s without any configuration", className);

        if (scala) {
            out.tab(1).println("def this() = {");
            out.tab(2).println("this(null)");
            out.tab(1).println("}");
        }
        else {
            out.tab(1).println("public %s() {", className);
            out.tab(2).println("super(%s, %s.class);", tableIdentifier, pType);
            out.tab(1).println("}");
        }

        // Initialising constructor
        // ------------------------

        if (scala) {
        }
        else {
            out.tab(1).javadoc("Create a new %s with an attached configuration", className);

            if (generateSpringAnnotations())
                out.tab(1).println("@%s", out.ref("org.springframework.beans.factory.annotation.Autowired"));

            out.tab(1).println("@%s", out.ref("javax.inject.Inject"));
            out.tab(1).println("public %s(%s configuration) {", className, Configuration.class);
            out.tab(2).println("super(%s, %s.class, configuration);", tableIdentifier, pType);
            out.tab(1).println("}");
        }

        // Template method implementations
        // -------------------------------
        if (scala) {
            out.println();
            out.tab(1).println("override def getId(o : %s) : %s = {", pType, tType);
        }
        else {
            out.tab(1).overrideInherit();
            out.tab(1).println("public %s getId(%s object) {", tType, pType);
        }

        if (keyColumns.size() == 1) {
            if (scala)
                out.tab(2).println("o.%s", getStrategy().getJavaGetterName(keyColumns.get(0), Mode.POJO));
            else
                out.tab(2).println("return object.%s();", getStrategy().getJavaGetterName(keyColumns.get(0), Mode.POJO));
        }

        // [#2574] This should be replaced by a call to a method on the target table's Key type
        else {
            String params = "";
            String separator = "";

            for (ColumnDefinition column : keyColumns) {
                if (scala)
                    params += separator + "o." + getStrategy().getJavaGetterName(column, Mode.POJO);
                else
                    params += separator + "object." + getStrategy().getJavaGetterName(column, Mode.POJO) + "()";

                separator = ", ";
            }

            if (scala)
                out.tab(2).println("compositeKeyRecord(%s)", params);
            else
                out.tab(2).println("return compositeKeyRecord(%s);", params);
        }

        out.tab(1).println("}");

        for (ColumnDefinition column : table.getColumns()) {
            final String colName = column.getOutputName();
            final String colClass = getStrategy().getJavaClassName(column);
            final String colTypeFull = getJavaType(column.getType(resolver()));
            final String colType = out.ref(colTypeFull);
            final String colIdentifier = executeJavaWriterRef(out, getStrategy().getFullJavaIdentifier(column), getColRefSegments(column));//out.ref(getStrategy().getFullJavaIdentifier(column), colRefSegments(column));

            // fetchRangeOf[Column]([T]...)
            // -----------------------
            if (!executePrintDeprecationIfUnknownType(out, colTypeFull))
                out.tab(1).javadoc("Fetch records that have <code>%s BETWEEN lowerInclusive AND upperInclusive</code>", colName);

            if (scala) {
                out.tab(1).println("def fetchRangeOf%s(lowerInclusive : %s, upperInclusive : %s) : %s[%s] = {", colClass, colType, colType, List.class, pType);
                out.tab(2).println("fetchRange(%s, lowerInclusive, upperInclusive)", colIdentifier);
                out.tab(1).println("}");
            }
            else {
                out.tab(1).println("public %s<%s> fetchRangeOf%s(%s lowerInclusive, %s upperInclusive) {", List.class, pType, colClass, colType, colType);
                out.tab(2).println("return fetchRange(%s, lowerInclusive, upperInclusive);", colIdentifier);
                out.tab(1).println("}");
            }

            // fetchBy[Column]([T]...)
            // -----------------------
            if (!executePrintDeprecationIfUnknownType(out, colTypeFull))
                out.tab(1).javadoc("Fetch records that have <code>%s IN (values)</code>", colName);

            if (scala) {
                out.tab(1).println("def fetchBy%s(values : %s*) : %s[%s] = {", colClass, colType, List.class, pType);
                out.tab(2).println("fetch(%s, values:_*)", colIdentifier);
                out.tab(1).println("}");
            }
            else {
                out.tab(1).println("public %s<%s> fetchBy%s(%s... values) {", List.class, pType, colClass, colType);
                out.tab(2).println("return fetch(%s, values);", colIdentifier);
                out.tab(1).println("}");
            }

            // fetchOneBy[Column]([T])
            // -----------------------
            ukLoop:
            for (UniqueKeyDefinition uk : column.getUniqueKeys()) {

                // If column is part of a single-column unique key...
                if (uk.getKeyColumns().size() == 1 && uk.getKeyColumns().get(0).equals(column)) {
                    if (!executePrintDeprecationIfUnknownType(out, colTypeFull))
                        out.tab(1).javadoc("Fetch a unique record that has <code>%s = value</code>", colName);

                    if (scala) {
                        out.tab(1).println("def fetchOneBy%s(value : %s) : %s = {", colClass, colType, pType);
                        out.tab(2).println("fetchOne(%s, value)", colIdentifier);
                        out.tab(1).println("}");
                    }
                    else {
                        out.tab(1).println("public %s fetchOneBy%s(%s value) {", pType, colClass, colType);
                        out.tab(2).println("return fetchOne(%s, value);", colIdentifier);
                        out.tab(1).println("}");
                    }

                    break ukLoop;
                }
            }
        }

        generateDaoClassFooter(table, out);
        out.println("}");
    }

    private int getColRefSegments(ColumnDefinition column) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method colRefSegments = JavaGenerator.class.getDeclaredMethod("colRefSegments", TypedElementDefinition.class);
        colRefSegments.setAccessible(true);

        return (int) colRefSegments.invoke(this, column);
    }

    private boolean executePrintDeprecationIfUnknownType(JavaWriter out, String type) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method printDeprecationIfUnknownType = JavaGenerator.class.getDeclaredMethod("printDeprecationIfUnknownType", JavaWriter.class, String.class);
        printDeprecationIfUnknownType.setAccessible(true);

        return (boolean) printDeprecationIfUnknownType.invoke(this, out, type);
    }

    private String executeJavaWriterRef(JavaWriter out, String fullJavaIdentifier, int i) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method ref = GeneratorWriter.class.getDeclaredMethod("ref", String.class, int.class);
        ref.setAccessible(true);

        return (String) ref.invoke(out, fullJavaIdentifier, i);
    }

    private <T> List<T> list(T... objects) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method list = JavaGenerator.class.getDeclaredMethod("list", Object[].class);
        list.setAccessible(true);

        return (List) list.invoke(this, objects);
    }

    private <T> List<T> list(T first, List<T> remaining) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method list = JavaGenerator.class.getDeclaredMethod("list", Object.class, List.class);
        list.setAccessible(true);

        return (List) list.invoke(this, first, remaining);
    }

    private List<? extends TypedElementDefinition<? extends Definition>> getTypedElements(Definition definition) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method getTypedElements = JavaGenerator.class.getDeclaredMethod("getTypedElements", Definition.class);
        getTypedElements.setAccessible(true);

        return (List) getTypedElements.invoke(this, definition);
    }

    private <T> List<T> first(Collection<T> objects) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method first = JavaGenerator.class.getDeclaredMethod("first", Collection.class);
        first.setAccessible(true);

        return (List) first.invoke(this, objects);
    }

    private <T> List<T> remaining(Collection<T> objects) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method remaining = JavaGenerator.class.getDeclaredMethod("remaining", Collection.class);
        remaining.setAccessible(true);

        return (List) remaining.invoke(this, objects);
    }

    private void printFromAndInto(JavaWriter out, Definition tableOrUDT) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method printFromAndInto = JavaGenerator.class.getDeclaredMethod("printFromAndInto", JavaWriter.class, Definition.class);
        printFromAndInto.setAccessible(true);

        printFromAndInto.invoke(this, out, tableOrUDT);
    }
}
