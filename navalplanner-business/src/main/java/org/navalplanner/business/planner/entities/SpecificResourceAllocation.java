/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
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

package org.navalplanner.business.planner.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.hibernate.validator.NotNull;
import org.joda.time.LocalDate;
import org.navalplanner.business.calendars.entities.AvailabilityTimeLine;
import org.navalplanner.business.calendars.entities.CombinedWorkHours;
import org.navalplanner.business.calendars.entities.IWorkHours;
import org.navalplanner.business.planner.entities.allocationalgorithms.HoursModification;
import org.navalplanner.business.planner.entities.allocationalgorithms.ResourcesPerDayModification;
import org.navalplanner.business.resources.daos.IResourceDAO;
import org.navalplanner.business.resources.entities.Resource;
import org.navalplanner.business.resources.entities.Worker;
import org.navalplanner.business.scenarios.entities.Scenario;
import org.navalplanner.business.util.deepcopy.OnCopy;
import org.navalplanner.business.util.deepcopy.Strategy;

/**
 * Represents the relation between {@link Task} and a specific {@link Worker}.
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
public class SpecificResourceAllocation extends
        ResourceAllocation<SpecificDayAssignment> implements IAllocatable {

    public static SpecificResourceAllocation create(Task task) {
        return create(new SpecificResourceAllocation(
                task));
    }

    @NotNull
    @OnCopy(Strategy.SHARE)
    private Resource resource;

    private Set<SpecificDayAssignmentsContainer> specificDayAssignmentsContainers = new HashSet<SpecificDayAssignmentsContainer>();

    public static SpecificResourceAllocation createForTesting(
            ResourcesPerDay resourcesPerDay, Task task) {
        return create(new SpecificResourceAllocation(
                resourcesPerDay, task));
    }

    /**
     * Constructor for hibernate. Do not use!
     */
    public SpecificResourceAllocation() {
        state = buildFromDBState();
    }

    private SpecificResourceAllocation(ResourcesPerDay resourcesPerDay,
            Task task) {
        super(resourcesPerDay, task);
        state = buildInitialTransientState();
    }

    private SpecificResourceAllocation(Task task) {
        super(task);
        state = buildInitialTransientState();
    }

    private DayAssignmentsState buildFromDBState() {
        return new SpecificDayAssignmentsNoExplicitlySpecifiedScenario();
    }

    private TransientState buildInitialTransientState() {
        return new TransientState(new HashSet<SpecificDayAssignment>());
    }

    public Resource getResource() {
        return resource;
    }

    private Map<Scenario, SpecificDayAssignmentsContainer> containersByScenario() {
        Map<Scenario, SpecificDayAssignmentsContainer> result = new HashMap<Scenario, SpecificDayAssignmentsContainer>();
        for (SpecificDayAssignmentsContainer each : specificDayAssignmentsContainers) {
            assert !result.containsKey(each);
            result.put(each.getScenario(), each);
        }
        return result;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public void allocate(ResourcesPerDay resourcesPerDay) {
        Validate.notNull(resourcesPerDay);
        Validate.notNull(resource);
        new SpecificAssignmentsAllocation().allocate(resourcesPerDay);
    }

    @Override
    public IAllocateResourcesPerDay until(LocalDate endExclusive) {
        return new SpecificAssignmentsAllocation().until(endExclusive);
    }

    @Override
    public IAllocateHoursOnInterval fromStartUntil(LocalDate endExclusive) {
        return new SpecificAssignmentsAllocation().fromStartUntil(endExclusive);
    }

    private final class SpecificAssignmentsAllocation extends
            AssignmentsAllocation {
        @Override
        protected List<SpecificDayAssignment> distributeForDay(
                LocalDate day, int totalHours) {
            return Arrays.asList(SpecificDayAssignment.create(day,
                    totalHours, resource));
        }

        @Override
        protected AvailabilityTimeLine getResourcesAvailability() {
            return AvailabilityCalculator.getCalendarAvailabilityFor(resource);
        }
    }

    @Override
    public IAllocateHoursOnInterval onInterval(LocalDate start, LocalDate end) {
        return new SpecificAssignmentsAllocation().onInterval(start, end);
    }

    @Override
    protected IWorkHours getWorkHoursGivenTaskHours(IWorkHours taskWorkHours) {
        return CombinedWorkHours.minOf(taskWorkHours, getResource()
                .getCalendar());
    }

    @Override
    protected Class<SpecificDayAssignment> getDayAssignmentType() {
        return SpecificDayAssignment.class;
    }

    public List<DayAssignment> createAssignmentsAtDay(LocalDate day,
            ResourcesPerDay resourcesPerDay, int limit) {
        int hours = calculateTotalToDistribute(day, resourcesPerDay);
        SpecificDayAssignment specific = SpecificDayAssignment.create(day, Math
                .min(limit, hours), resource);
        List<DayAssignment> result = new ArrayList<DayAssignment>();
        result.add(specific);
        return result;
    }

    @Override
    public IAllocatable withPreviousAssociatedResources() {
        return this;
    }

    @Override
    public List<Resource> getAssociatedResources() {
        return Arrays.asList(resource);
    }

    @Override
    ResourceAllocation<SpecificDayAssignment> createCopy(Scenario scenario) {
        SpecificResourceAllocation result = create(getTask());
        result.toTransientStateWithInitial(getUnorderedFor(scenario));
        result.resource = getResource();
        return result;
    }

    private void toTransientStateWithInitial(
            Set<SpecificDayAssignment> initialAssignments) {
        this.state = new TransientState(initialAssignments);
    }

    @Override
    public ResourcesPerDayModification asResourcesPerDayModification() {
        return ResourcesPerDayModification.create(this, getResourcesPerDay());
    }

    @Override
    public HoursModification asHoursModification() {
        return HoursModification.create(this, getAssignedHours());
    }

    @Override
    public ResourcesPerDayModification withDesiredResourcesPerDay(
            ResourcesPerDay resourcesPerDay) {
        return ResourcesPerDayModification.create(this, resourcesPerDay);
    }

    @Override
    public List<Resource> querySuitableResources(IResourceDAO resourceDAO) {
        return Collections.singletonList(resource);
    }

    private class ExplicitlySpecifiedScenarioState extends DayAssignmentsState {

        private SpecificResourceAllocation outerSpecificAllocation = SpecificResourceAllocation.this;
        private final SpecificDayAssignmentsContainer container;

        private ExplicitlySpecifiedScenarioState(Scenario scenario) {
            Validate.notNull(scenario);
            this.container = retrieveOrCreateContainerFor(scenario);
        }

        private SpecificDayAssignmentsContainer retrieveOrCreateContainerFor(
                Scenario scenario) {
            Map<Scenario, SpecificDayAssignmentsContainer> containers = containersByScenario();
            SpecificDayAssignmentsContainer retrieved = containers
                    .get(scenario);
            if (retrieved != null) {
                return retrieved;
            }
            SpecificDayAssignmentsContainer result = SpecificDayAssignmentsContainer
                    .create(outerSpecificAllocation, scenario);
            specificDayAssignmentsContainers.add(result);
            return result;
        }

        @Override
        protected void addAssignments(
                Collection<? extends SpecificDayAssignment> assignments) {
            container.addAll(assignments);
        }

        @Override
        protected void clearFieldsCalculatedFromAssignments() {
        }

        @Override
        protected Collection<SpecificDayAssignment> copyAssignmentsFrom(
                ResourceAllocation<?> modification) {
            SpecificResourceAllocation specificModication = (SpecificResourceAllocation) modification;
            return SpecificDayAssignment.copy(outerSpecificAllocation,
                    specificModication.getAssignments());
        }

        @Override
        protected Collection<SpecificDayAssignment> getUnorderedAssignments() {
            return container.getDayAssignments();
        }

        @Override
        protected void removeAssignments(
                List<? extends DayAssignment> assignments) {
            container.removeAll(assignments);
        }

        @Override
        protected void resetTo(
                Collection<SpecificDayAssignment> assignmentsCopied) {
            container.resetTo(assignmentsCopied);
        }

        @Override
        protected void setParentFor(SpecificDayAssignment each) {
            each.setSpecificResourceAllocation(outerSpecificAllocation);
        }

        @Override
        protected DayAssignmentsState switchTo(Scenario scenario) {
            ExplicitlySpecifiedScenarioState result = new ExplicitlySpecifiedScenarioState(
                    scenario);
            result.resetTo(container.getDayAssignments());
            return result;
        }
    }

    private class TransientState extends DayAssignmentsState {
        private SpecificResourceAllocation outerSpecificAllocation = SpecificResourceAllocation.this;

        private final Set<SpecificDayAssignment> specificDaysAssignment;

        TransientState(Set<SpecificDayAssignment> specificDayAssignments) {
            this.specificDaysAssignment = specificDayAssignments;
        }

        @Override
        protected void addAssignments(
                Collection<? extends SpecificDayAssignment> assignments) {
            specificDaysAssignment.addAll(assignments);
        }

        @Override
        protected void clearFieldsCalculatedFromAssignments() {
        }

        @Override
        protected Collection<SpecificDayAssignment> copyAssignmentsFrom(
                ResourceAllocation<?> modification) {
            SpecificResourceAllocation specificModication = (SpecificResourceAllocation) modification;
            return SpecificDayAssignment.copy(outerSpecificAllocation,
                    specificModication.getAssignments());
        }

        @Override
        protected Collection<SpecificDayAssignment> getUnorderedAssignments() {
            return specificDaysAssignment;
        }

        @Override
        protected void removeAssignments(
                List<? extends DayAssignment> assignments) {
            specificDaysAssignment.removeAll(assignments);
        }

        @Override
        protected void resetTo(
                Collection<SpecificDayAssignment> assignmentsCopied) {
            specificDaysAssignment.clear();
            specificDaysAssignment.addAll(assignmentsCopied);
        }

        @Override
        protected void setParentFor(SpecificDayAssignment each) {
            each.setSpecificResourceAllocation(outerSpecificAllocation);
        }

        @Override
        protected DayAssignmentsState switchTo(Scenario scenario) {
            ExplicitlySpecifiedScenarioState result = new ExplicitlySpecifiedScenarioState(
                    scenario);
            result.resetTo(specificDaysAssignment);
            return result;
        }
    }

    private Set<SpecificDayAssignment> getUnorderedFor(Scenario scenario) {
        SpecificDayAssignmentsContainer container = containersByScenario()
                .get(scenario);
        if (container == null) {
            return new HashSet<SpecificDayAssignment>();
        }
        return container.getDayAssignments();
    }

    private class SpecificDayAssignmentsNoExplicitlySpecifiedScenario extends
            NoExplicitlySpecifiedScenario {

        @Override
        protected Collection<SpecificDayAssignment> getUnorderedAssignmentsForScenario(
                Scenario scenario) {
            return getUnorderedFor(scenario);
        }

        @Override
        protected DayAssignmentsState switchTo(Scenario scenario) {
            return new ExplicitlySpecifiedScenarioState(scenario);
        }

    }

    @OnCopy(Strategy.IGNORE)
    private DayAssignmentsState state;

    @Override
    protected void scenarioChangedTo(Scenario scenario) {
        this.state = getDayAssignmentsState().switchTo(scenario);
    }

    @Override
    protected ResourceAllocation<SpecificDayAssignment>.DayAssignmentsState getDayAssignmentsState() {
        return state;
    }

}
