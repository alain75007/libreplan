package org.zkoss.ganttz.extensions;

import org.zkoss.ganttz.Planner;
import org.zkoss.ganttz.TaskComponent;
import org.zkoss.ganttz.TimeTracker;
import org.zkoss.ganttz.adapters.IAdapterToTaskFundamentalProperties;
import org.zkoss.ganttz.adapters.IStructureNavigator;
import org.zkoss.ganttz.adapters.PlannerConfiguration;
import org.zkoss.ganttz.data.Position;
import org.zkoss.ganttz.data.Task;
import org.zkoss.zk.ui.Component;

/**
 * A facade for operations allowed to extensions <br />
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
public interface IContext<T> {

    /**
     * Adding domainObject to the {@link Planner}. It is transformed using
     * {@link IAdapterToTaskFundamentalProperties} and
     * {@link IStructureNavigator}. It appends the created {@link Task} to the
     * top of the planner
     * @param domainObject
     */
    void add(T domainObject);

    /**
     * Reloading all the {@link Planner} from the configuration
     * @param configuration
     */
    void reload(PlannerConfiguration<?> configuration);

    /**
     * Removing the tasks associated to the domainObject
     * @param domainObject
     * @return the Position in which the domainObject's task was
     */
    Position remove(T domainObject);

    /**
     * Retrieves the component associated to the action performed. Normally it
     * is the {@link Planner}, but it can be other. For example, if the action
     * is performed on a {@link TaskComponent} this method might return the said
     * component.
     * @return the component the action is relative to
     */
    public Component getRelativeTo();

    /**
     * Removes the oldDomainObject, and adds newDomainObject in the position
     * occupied by oldDomainObject.<br />
     * This method might be useful when you modify a domainObject and you want
     * it to be reloaded again
     * @param oldDomainObject
     *            the domain object associated to the task that is going to be
     *            removed
     * @param newDomainObject
     *            the domain object from which a task will be created and
     *            positioned in the place of oldDomainObject
     */
    void replace(T oldDomainObject, T newDomainObject);

    /**
     * Inserts a new task created from domainObject on the position specified.
     * @param position
     *            The position in which the insertion will be done
     * @param domainObject
     *            the domain object from which a task will be created
     */
    void add(Position position, T domainObject);

    /**
     * Makes the time tracker available.
     * @return the in use {@link TimeTracker Time Tracker}
     */
    TimeTracker getTimeTracker();

}
