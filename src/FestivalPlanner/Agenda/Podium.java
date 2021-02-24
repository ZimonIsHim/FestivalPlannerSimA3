package FestivalPlanner.Agenda;

import java.io.Serializable;

/**
 * Contains all information relevant to a stage, such as the name and location.
 */
public class Podium implements Serializable {

    private String name;
    private String location;

    /**
     * Constructor for the <code>Podium</code> class.
     * @param name  sets <code>this.name</code> to the given parameter's value
     * @param location  sets <code>this.location</code> to the given parameter's value
     */
    public Podium(String name, String location) {
        this.name = name;
        this.location = location;
    }

    /**
     * Returns a string containing the name of this stage.
     * @return  this.name, a string containing this name of this stage
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns a string containing the location of this stage.
     * @return  this.location, a string containing this location of this stage
     */
    public String getLocation() {
        return this.location;
    }
}