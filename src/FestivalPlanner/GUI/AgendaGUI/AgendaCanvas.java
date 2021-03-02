package FestivalPlanner.GUI.AgendaGUI;

import FestivalPlanner.Agenda.Agenda;
import FestivalPlanner.Agenda.Show;
import FestivalPlanner.Agenda.Podium;
import FestivalPlanner.Util.PreferencesHandling.SaveSettingsHandler;
import com.sun.istack.internal.Nullable;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import org.jfree.fx.FXGraphics2D;
import org.jfree.fx.ResizableCanvas;
import java.awt.geom.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Class that will draw a Agenda to a canvas.
 */
public class AgendaCanvas {

    private AgendaModule agendaModule;
    private Canvas canvas;
    private BorderPane mainPane = new BorderPane();
    private AffineTransform cameraTransform;
    private double scale = 1.0;
    private ArrayList<ShowRectangle2D> showRectangles;

    private int startX;
    private int endX;
    private int startY;
    private int endY;

    private Agenda agenda;
    private ArrayList<Podium> usedStages;

    /**
     * Blank constructor of <code>AgendaCanvas</code>.
     */
    public AgendaCanvas(AgendaModule agendaModule) {
        this(new Agenda(), 1920 / 4f, 1080 / 3f, agendaModule);
    }

    /**
     * Constructor of <code>AgendaCanvas</code>.
     * <p>
     * Uses the Agenda given as parameter for <code>this.agenda</code>.
     * @param agenda  sets <code>this.agenda</code> to this object
     */
    public AgendaCanvas(Agenda agenda, AgendaModule agendaModule) {
        this(agenda, 1920 / 4f, 1080 / 3f, agendaModule);
    }

    /**
     * Constructor of <code>AgendaCanvas</code>.
     * <p>
     * Uses the the given width and height for <code>this.canvas.setWidth</code> and <code>this.canvas.setHeight</code>
     * respectively. It creates a new blank Agenda for <code>this.agenda</code>.
     *
     * @param width  sets <code>this.canvas.setWidth</code> to this value
     * @param height  sets <code>this.canvas.setHeight</code> to this value
     */
    public AgendaCanvas(double width, double height, AgendaModule agendaModule) {
        this(new Agenda(), width, height, agendaModule);
    }

    /**
     * Top constructor of <code>AgendaCanvas</code>.
     * <p>
     * Initializes all the attributes of <code>AgendaCanvas</code>.
     * <p>
     * Last action is calling the <code>draw()</code> method,
     * so the canvas will be drawn after initialisation of <code>AgendaCanvas</code>.
     *
     * @param width  sets <code>this.canvas.setWidth</code> to this value
     * @param height  sets <code>this.canvas.setHeight</code> to this value
     */
    public AgendaCanvas(Agenda agenda, double width, double height, AgendaModule agendaModule) {
        this.agendaModule = agendaModule;
        this.agenda = agenda;

        this.usedStages = calculateUsedStages();
        this.showRectangles = showRectanglesToArrayList();

        this.cameraTransform = new AffineTransform();
        this.canvas = new ResizableCanvas(this::draw, this.mainPane);
        this.canvas.setHeight(height);
        this.canvas.setWidth(width);
        this.canvas.setOnScroll(this::setOnScroll);

        calculateBounds();

        this.mainPane.setCenter(this.canvas);
        FXGraphics2D graphics = new FXGraphics2D(canvas.getGraphicsContext2D());
        draw(graphics);
    }
    /**
     * Public method that builds the AgendaCanvas and then redraws.
     * <p>
     * See {@link #buildAgendaCanvas()}.
     */
    public void reBuildAgendaCanvas() {
        draw(new FXGraphics2D(this.canvas.getGraphicsContext2D()));
    }

    /**
     * Getter for <code>this.mainPane</code>.
     * @return  this.mainPane
     */
    public Node getMainPane() {
        buildAgendaCanvas();
        return mainPane;
    }

    /**
     * Getter for <code>this.agenda</code>.
     * @return  Value of <code>this.agenda</code>
     */
    public Agenda getAgenda() {
        return agenda;
    }

    /**
     * Setter for <code>this.agenda</code>.
     * <p>
     * @param agenda  sets <code>this.agenda</code> to this value.
     */
    public void setAgenda(Agenda agenda) {
        this.agenda = agenda;
        buildAgendaCanvas();
        draw(new FXGraphics2D(this.canvas.getGraphicsContext2D()));
    }

    /**
     * Getters for <code>this.canvas</code>.
     * @return  <code>this.canvas</code>
     */
    public Canvas getCanvas() {
        return canvas;
    }

    /**
     * Looks if a <a href="{@docRoot}/FestivalPlanner/Agenda/Show.html">Show</a> is displayed at the given
     * <a href="https://docs.oracle.com/javase/7/docs/api/java/awt/geom/Point2D.html">Point2D</a>.
     * Returns null when there is no <a href="{@docRoot}/FestivalPlanner/GUI/ShowRectangle2D.html">ShowRectangle2D</a>
     * at given <a href="https://docs.oracle.com/javase/7/docs/api/java/awt/geom/Point2D.html">Point2D</a>
     * @param point  The point to check is there is a drawn <a href="{@docRoot}/FestivalPlanner/GUI/ShowRectangle2D.html">ShowRectangle2D</a>
     * @return  The <a href="{@docRoot}/FestivalPlanner/Agenda/Show.html">Show</a> that the <a href="{@docRoot}/FestivalPlanner/GUI/ShowRectangle2D.html">ShowRectangle2D</a> at the given
     * <a href="https://docs.oracle.com/javase/7/docs/api/java/awt/geom/Point2D.html">Point2D</a> represents.
     */
    @Nullable
    public Show showAtPoint(Point2D point) {
        //Adjust the point to the cameratransform and startTranslate.
        Point2D adjustedPoint = new Point2D.Double(point.getX() + this.startX - this.cameraTransform.getTranslateX(),
                point.getY() + this.startY - this.cameraTransform.getTranslateY());

        for (ShowRectangle2D showRectangle : this.showRectangles) {
            if (showRectangle.getRectangle().contains(adjustedPoint)) {
                return showRectangle.getShow();
            }
        }
        return null;
    }

    /**
     * When this method is called it calculates and sets the following attributes:
     * <p>
     * <ul>
     * <li>{@link #calculateBounds()}</li>
     * <li>{@link #calculateUsedStages()}</li>
     * <li>{@link #showRectanglesToArrayList()}</li>
     * </ul>
     * <p>
     * When an update has been made to <code>this.agenda</code> this method will make the canvas recalculate the
     * things linked above.
     * <p>
     * This method wil also be called in the constructor of this object and after setting <code>this.agenda</code>.
     * After these actions it is unnecessary to call this method.
     */
    private void buildAgendaCanvas() {
        calculateBounds();
        this.usedStages = calculateUsedStages();
        this.showRectangles = showRectanglesToArrayList();
    }

    /**
     * Creates an ArrayList with a <a href="https://docs.oracle.com/javase/8/docs/api/java/awt/geom/Rectangle2D.html">Rectangle2D</a> for every <a href="{@docRoot}/FestivalPlanner/Agenda/Show.html">Show</a>.
     * @return  ArrayList with all the <a href="https://docs.oracle.com/javase/8/docs/api/java/awt/geom/Rectangle2D.html">rectangles</a> from the <a href="{@docRoot}/FestivalPlanner/Agenda/Show.html">shows</a>
     */
    private ArrayList<ShowRectangle2D> showRectanglesToArrayList() {
        ArrayList<ShowRectangle2D> rectangles = new ArrayList<>();
        for (Show show : this.agenda.getShows()) {
            rectangles.add(createShowRectangle(show));
        }
        return rectangles;
    }

    /**
     * Creates an Rectangle that represents the given <a href="{@docRoot}/FestivalPlanner/Agenda/Show.html">show</a>.
     * @param show  The <a href="{@docRoot}/FestivalPlanner/Agenda/Show.html">show</a> that the returned <a href="https://docs.oracle.com/javase/8/docs/api/java/awt/geom/Rectangle2D.html">rectangle</a> is based on
     * @return  a rectangle, this rectangle isn't shown yet but has a size and location based on the given <a href="{@docRoot}/FestivalPlanner/Agenda/Show.html">show</a>
     */
    private ShowRectangle2D createShowRectangle(Show show) {
        ArrayList<Show> selectedShows = this.agendaModule.getSelectedShows();

        double hourLength = 60 * this.scale;
        double startTime = show.getStartTime().getHour() + (show.getStartTime().getMinute() / 60f);
        double endTime = show.getEndTime().getHour() + (show.getEndTime().getMinute() / 60f);
        int stageIndex = this.usedStages.indexOf(show.getPodium());

        Rectangle2D rectangle2D = new Rectangle2D.Double(startTime * hourLength, stageIndex * 80 + 5, (endTime * hourLength) - (startTime * hourLength), 70);
        if (selectedShows.contains(show)) {
            return new ShowRectangle2D(rectangle2D, show, SaveSettingsHandler.unselectedColor);
        }
        return new ShowRectangle2D(rectangle2D, show);
    }

    /**
     * Sets the boundaries of the agendaCanvas.
     * <p>
     * Initializes <code>this.startX</code>, <code>this.endX</code>, <code>this.startY</code>, <code>this.endY</code> based on the calculated boundaries.
     */
    private void calculateBounds() {
        this.startX = -100;
        this.startY = -50;

        double monitorToScale = (this.canvas.getWidth() + startX) / (24 * 60 * scale);
        if (monitorToScale > 1 ) {
            this.scale = (this.canvas.getWidth() + startX) / (24 * 60);
        }

        if (this.canvas.getHeight() > this.usedStages.size() * 80) {
            this.endY = (int)this.canvas.getHeight();
        } else {
            this.endY = this.usedStages.size() * 80 + 50;
        }

        this.endX = (int)(60 * this.scale * 24);
    }

    /**
     * Gets a list of all the used <a href="{@docRoot}/FestivalPlanner/Agenda/Stages.html">Stages</a> in <code>this.agenda</code>, duplicates won't show up.
     * @return  an ArrayList with all the used <a href="{@docRoot}/FestivalPlanner/Agenda/Stages.html">Stages</a> in <code>this.agenda</code>
     */
    private ArrayList<Podium> calculateUsedStages() {
        Set<Podium> stageSet = new HashSet<>();

        for (Show show : this.agenda.getShows()) {
            stageSet.add(show.getPodium());
        }
        return new ArrayList<>(stageSet);
    }

    /**
     * Draws everything on <code>this.canvas</code>.
     * @param graphics  object that draws on <code>this.canvas</code>
     */
    private void draw(FXGraphics2D graphics) {
        //Resetting the screen.
        graphics.setTransform(new AffineTransform());
        graphics.setBackground(Color.white);
        graphics.clearRect(0, 0, (int) canvas.getWidth(), (int) canvas.getHeight());

        graphics.setTransform(this.cameraTransform);
        graphics.translate(-this.startX, -this.startY);

        buildAgendaCanvas();
        drawTopBar(graphics);
        drawStages(graphics);

        for (ShowRectangle2D showRectangle : this.showRectangles) {
            showRectangle.draw(graphics);
            Area overlap = getIntersectArea(showRectangle.getRectangle());
            if (overlap != null) {
                graphics.setColor(Color.getHSBColor(0, .7f, .9f));
                graphics.fill(overlap);
                graphics.setColor(Color.BLACK);
                graphics.draw(overlap);
            }
        }

    }

    //TODO: Add documentation.
    @Nullable
    private Area getIntersectArea(Rectangle2D mainRect) {
        for (ShowRectangle2D otherShowRect : this.showRectangles) {
            if (otherShowRect.getRectangle() != mainRect && mainRect.intersects(otherShowRect.getRectangle())) {
                Area mainArea = new Area(mainRect);
                Area otherArea = new Area(otherShowRect.getRectangle());
                mainArea.intersect(otherArea);
                return mainArea;
            }
        }
        return null;
    }

    /**
     * Draws the time information on the horizontal axis to <code>this.canvas</code>.
     * @param graphics  object to draw on
     */
    private void drawTopBar(FXGraphics2D graphics) {
        graphics.drawLine(this.startX, 0, this.endX, 0);
        graphics.drawLine(0, this.startY, 0, this.endY);

        double hourLenght = 60 * this.scale;

        graphics.setColor(Color.getHSBColor(0, 0, 0.75f));
        graphics.fill(new Rectangle2D.Double(0, 0, hourLenght / 2, this.endY));
        graphics.setColor(Color.BLACK);

        for (int i = 0; i < 24; i++) { //Later changed in starttime till endtime
            int x = (int)(i * hourLenght + hourLenght);
            graphics.drawString(i + ".00", x - ((int)hourLenght / 2 ) - 15, -25); //numbers will later be based on cameraTransform etc.
            graphics.setColor(Color.lightGray);
            graphics.drawLine(x, this.startY, x, this.endY);
            graphics.setColor(Color.getHSBColor(0, 0, 0.75f));
            graphics.fill(new Rectangle2D.Double(x, 0, hourLenght / 2, this.endY));
            graphics.setColor(Color.BLACK);
        }
    }

    /**
     * Draws the lines and names for all <a href="{@docRoot}/FestivalPlanner/Agenda/Stages.html">stages</a> in <code>this.usedStages</code>.
     * @param graphics  object to draw on
     */
    private void drawStages(FXGraphics2D graphics) {
        int stageHeight = 80;
        ArrayList<Podium> usedStages1 = this.usedStages;
        for (int i = 0; i < usedStages1.size(); i++) {
            Podium stage = usedStages1.get(i);
            graphics.drawLine(this.startX, stageHeight * (i + 1), this.endX, stageHeight * (i + 1));
            graphics.drawString(stage.getName(), this.startX + 10, stageHeight * (i + 1) - stageHeight / 2);
        }
    }

    /**
     * Handles the ScrollEvent for vertical and horizontal scrolling.
     * @param scrollEvent is the eventhandler for scrolling
     */
    private void setOnScroll(ScrollEvent scrollEvent) {
        AffineTransform transform = new AffineTransform();

        if (scrollEvent.isAltDown()) {
            double zoomFactor = 1 + (scrollEvent.getDeltaY()/1000);
            transform.scale(zoomFactor, 1);
            this.scale *= zoomFactor;
            this.cameraTransform.translate(-this.cameraTransform.getTranslateX(), -this.cameraTransform.getTranslateY());
        } else {

            double scrollPixelsY = scrollEvent.getDeltaY() / 1.5;
            double scrollPixelsX = scrollEvent.getDeltaX() / 1.5;
            transform.translate(scrollPixelsX, scrollPixelsY);
            if (cameraInBounds(transform)) {
                this.cameraTransform.translate(scrollPixelsX, scrollPixelsY);
            }

        }

        draw(new FXGraphics2D(this.canvas.getGraphicsContext2D()));
    }

    /**
     * Calculates if the given translate will fit within the set bounds.
     * <p>
     * Currently only works on translations, scale not yet implemented.
     * @param transform  AffineTransform that is proposed
     * @return  true if the given translate is in bounds
     */
    private boolean cameraInBounds(AffineTransform transform) {
        return (this.cameraTransform.getTranslateX() + transform.getTranslateX() <= 1 &&
                this.cameraTransform.getTranslateX() + transform.getTranslateX() >= -(this.endX - this.startX - this.canvas.getWidth()) &&
                this.cameraTransform.getTranslateY() + transform.getTranslateY() <= 1 &&
                this.cameraTransform.getTranslateY() + transform.getTranslateY() >= -(this.endY - this.startY - this.canvas.getHeight())
        );
    }

}
