/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.navalplanner.web.costcategories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.navalplanner.business.common.IntegrationEntity;
import org.navalplanner.business.common.entities.EntityNameEnum;
import org.navalplanner.business.costcategories.entities.CostCategory;
import org.navalplanner.business.costcategories.entities.ResourcesCostCategoryAssignment;
import org.navalplanner.business.resources.daos.IResourceDAO;
import org.navalplanner.business.resources.entities.Resource;
import org.navalplanner.web.common.IntegrationEntityModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Model for UI operations related to {@link ResourcesCostCategoryAssignment}
 *
 * @author Jacobo Aragunde Perez <jaragunde@igalia.com>
 * @author Diego Pino Garcia <dpino@igalia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ResourcesCostCategoryAssignmentModel extends
        IntegrationEntityModel implements
        IResourcesCostCategoryAssignmentModel {

    private Resource resource;

    private ResourcesCostCategoryAssignment currentAssignment;

    @Autowired
    IResourceDAO resourceDAO;

    private List<ResourcesCostCategoryAssignment> resourceCostCategoryAssignments =
        new ArrayList<ResourcesCostCategoryAssignment>();

    @Override
    public List<ResourcesCostCategoryAssignment> getCostCategoryAssignments() {
        if (resource != null) {
            loadCostCategoryAssignments();
        }
        return resourceCostCategoryAssignments;
    }

    private void loadCostCategoryAssignments() {
        resourceCostCategoryAssignments.clear();
        resourceCostCategoryAssignments.addAll(resource.getResourcesCostCategoryAssignments());
    }

    @Override
    public void addCostCategory() {
        ResourcesCostCategoryAssignment assignment = ResourcesCostCategoryAssignment
                .create();
        resource.addResourcesCostCategoryAssignment(assignment);

        // set the autogenerated code
        this.currentAssignment = assignment;
        this.setDefaultCode();
    }

    @Override
    public void removeCostCategoryAssignment(
            ResourcesCostCategoryAssignment assignment) {
        resource.removeResourcesCostCategoryAssignment(assignment);
        loadCostCategoryAssignments();
    }

    @Override
    @Transactional(readOnly = true)
    public void setResource(Resource resource) {
        resourceDAO.reattach(resource);
        initializeCostCategoryAssignments(resource.getResourcesCostCategoryAssignments());
        this.resource = resource;
    }

    private void initializeCostCategoryAssignments(Collection<ResourcesCostCategoryAssignment> resourceCostCategoryAssignments) {
        for (ResourcesCostCategoryAssignment each: resourceCostCategoryAssignments) {
            initializeCostCategoryAssignment(each);
        }
    }

    private void initializeCostCategoryAssignment(ResourcesCostCategoryAssignment resourceCostCategoryAssignment) {
        resourceCostCategoryAssignment.getEndDate();
        initializeCostCategory(resourceCostCategoryAssignment.getCostCategory());
    }

    private void initializeCostCategory(CostCategory costCategory) {
        costCategory.getName();
    }

    @Override
    protected Set<IntegrationEntity> getChildren() {
        return new HashSet<IntegrationEntity>();
    }

    @Override
    public IntegrationEntity getCurrentEntity() {
        return currentAssignment;
    }

    @Override
    public EntityNameEnum getEntityName() {
        return EntityNameEnum.RESOURCE_COST_CATEGORY_ASSIGNMENT;
    }

}
