/*
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.rest.api.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.rest.api.NodeDefinitionMapper;
import org.alfresco.rest.api.model.NodeDefinitionConstraint;
import org.alfresco.rest.api.model.NodeDefinition;
import org.alfresco.rest.api.model.NodeDefinitionProperty;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.i18n.MessageLookup;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

public class NodeDefinitionMapperImpl implements NodeDefinitionMapper {

    private final List<String> EXCLUDED_NS = Arrays.asList(NamespaceService.SYSTEM_MODEL_1_0_URI);

    @Override
    public NodeDefinition fromTypeDefinition(String nodeTypeId, TypeDefinition typeDefinition,
            MessageLookup messageLookup) {
        
        if (typeDefinition == null) {
            throw new AlfrescoRuntimeException("Impossible to retrieve the node definition");
        }
        NodeDefinition nodeDefinition = new NodeDefinition();
        nodeDefinition.setTypeId(nodeTypeId);
        nodeDefinition.setTitle(typeDefinition.getTitle(messageLookup));
        nodeDefinition.setParentTypeId(getParentTypeId(typeDefinition.getParentName()));
        nodeDefinition.setDescription(typeDefinition.getDescription(messageLookup));
        nodeDefinition.setProperties(getProperties(typeDefinition.getProperties(), messageLookup));
        
        return nodeDefinition;
    }
        
    private String getParentTypeId(QName parentName) {
        String parentTypeId = null;
        if (parentName != null){
            parentTypeId = parentName.getPrefixString();
        }
        return parentTypeId;
    }

    private boolean isPropertyExcluded(QName propertyName) {
        return EXCLUDED_NS.contains(propertyName.getNamespaceURI());
    }

    private List <NodeDefinitionProperty> getProperties(Map<QName, PropertyDefinition> propertiesMap, MessageLookup messageLookup){
        return propertiesMap.values().stream()
                .filter(p -> !isPropertyExcluded(p.getName()))
                .map(p -> fromPropertyDefinitionToProperty(p , messageLookup))
                .collect(Collectors.toList());
    }
    
    private NodeDefinitionProperty fromPropertyDefinitionToProperty(PropertyDefinition propertyDefinition,
            MessageLookup messageLookup){
        
        if (propertyDefinition == null) {
            throw new AlfrescoRuntimeException("Impossible to retrieve properties for the node definition");
        }
        NodeDefinitionProperty property = new NodeDefinitionProperty();
        property.setId(propertyDefinition.getName().toPrefixString());
        property.setTitle(propertyDefinition.getTitle(messageLookup));
        property.setDescription(propertyDefinition.getDescription(messageLookup));
        property.setDefaultValue(propertyDefinition.getDefaultValue());
        property.setDataType(propertyDefinition.getDataType().getName().toPrefixString());
        property.setIsOverride(propertyDefinition.isOverride());
        property.setIsMultiValued(propertyDefinition.isMultiValued());
        property.setIsMandatory(propertyDefinition.isMandatory());
        property.setIsMandatoryEnforced(propertyDefinition.isMandatoryEnforced());
        property.setIsProtected(propertyDefinition.isProtected());
        property.setConstraints(getConstraints(propertyDefinition.getConstraints(), messageLookup));
       
        return property;
    }
    
    private List<NodeDefinitionConstraint> getConstraints( Collection<ConstraintDefinition> constraintDefinitions,
            MessageLookup messageLookup) {

        return constraintDefinitions.stream()
                .map(constraint -> fromConstraintDefinitionToConstraint(constraint, messageLookup))
                .collect(Collectors.toList());
    }

    private NodeDefinitionConstraint fromConstraintDefinitionToConstraint(ConstraintDefinition constraintDefinition, 
            MessageLookup messageLookup) {

        if (constraintDefinition == null || constraintDefinition.getConstraint() == null) {
            throw new AlfrescoRuntimeException("Impossible to retrieve constraints for the node definition");
        }
        NodeDefinitionConstraint constraint = new NodeDefinitionConstraint();
        constraint.setId(constraintDefinition.getConstraint().getShortName());
        constraint.setType(constraintDefinition.getConstraint().getType());
        constraint.setTitle(constraintDefinition.getTitle(messageLookup));
        constraint.setDescription(constraintDefinition.getDescription(messageLookup));
        constraint.setParameters(constraintDefinition.getConstraint().getParameters());
        
        return constraint;
    }

}
