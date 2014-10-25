@TypeDefs({
        @TypeDef(name = "genderType",
                defaultForType = Gender.class,
                typeClass = EnumUserType.class,
                parameters = @Parameter(name = "enumClassName",
                        value = "de.test.schemaexport.domain.Gender"))
})
package de.test.schemaexport.domain;

import de.test.schemaexport.domain.EnumUserType;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;