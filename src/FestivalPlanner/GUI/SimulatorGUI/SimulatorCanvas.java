package FestivalPlanner.GUI.SimulatorGUI;

import FestivalPlanner.GUI.AbstractGUI;
import FestivalPlanner.NPC.NPC;
import FestivalPlanner.TileMap.TileMap;
import FestivalPlanner.Util.JsonHandling.JsonConverter;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import org.jfree.fx.FXGraphics2D;
import org.jfree.fx.ResizableCanvas;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;

/**
 * Class that will draw a <a href="{@docRoot}/FestivalPlanner/TileMap/TileMap.html">TileMap</a> to a canvas.
 */
public class SimulatorCanvas extends AbstractGUI {

        private Canvas canvas;
        private SimulatorModule simulatorModule;
        private BorderPane mainPane;
        private AffineTransform cameraTransform;
        private final int CAMERA_SPEED = 30;

        private int startX;
        private int endX;
        private int startY;
        private int endY;

        private ArrayList<NPC> npcList = new ArrayList<>(40);

        //@TODO add simulatorHandler

        //TESTING PURPOSES
        JsonConverter converter = new JsonConverter();
        private TileMap tileMap = converter.JSONToTileMap("/testMap.json");

    /**
     * Constructor for SimulatorCanvas
     * @param simulatorModule  The <a href="{@docRoot}/FestivalPlanner/GUI/SimulatorGUI/SimulatorModule.html">SimulatorModule</a> that contains this canvas
     * @param canvasWidth  The initial width of the canvas
     * @param canvasHeight  The initial height of this canvas
     */
    public SimulatorCanvas(SimulatorModule simulatorModule, double canvasWidth, double canvasHeight) {
        this.simulatorModule = simulatorModule;
        this.mainPane = new BorderPane();
        this.canvas = new ResizableCanvas(this::draw, this.mainPane);
        this.canvas.setWidth(canvasWidth);
        this.canvas.setHeight(canvasHeight);
        load();
    }

    @Override
    public void load() {
        setup();
        this.actionHandlingSetup();

        FXGraphics2D g2d = new FXGraphics2D(this.canvas.getGraphicsContext2D());

        new AnimationTimer() {
            long last = -1;

            @Override
            public void handle(long now) {
                if (last == -1) {
                    last = now;
                }
                update((now - last) / 1000000000.0);
                last = now;
                draw(g2d);
            }
        }.start();

        draw(new FXGraphics2D(this.canvas.getGraphicsContext2D()));
        this.canvas.setFocusTraversable(true);
    }

    @Override
    public void setup() {

        Random r = new Random(0);
        while(this.npcList.size() < 100)
        {
            NPC npc = new NPC(new Point2D.Double((int)(Math.random() * 1000), (int)(Math.random() * 1000)), r.nextInt(NPC.getCharacterFiles()));
            if(!npc.checkCollision(this.npcList))
            {
                this.npcList.add(npc);
            }
        }

        this.mainPane.setCenter(this.canvas);
        this.startX = 0;
        this.endX= tileMap.getMapWidth() * tileMap.getTileWidth();
        this.startY = 0;
        this.endY = tileMap.getMapHeight() * tileMap.getTileHeight();
        this.cameraTransform = new AffineTransform();
    }

    @Override
    public void actionHandlingSetup() {
        //@TODO setonmousclick etc..
        this.canvas.setOnKeyPressed(this::onWASD);
        this.canvas.setOnMouseDragged(this::onMouseDragged);
        this.canvas.setOnMousePressed(this::onMousePressed);
        this.canvas.setOnMouseReleased(this::onMouseReleased);
    }

    /**
     * Getter for <code>this.mainPane</code>
     * @return  <code>this.mainPane</code>
     */
    public BorderPane getMainPane() {
        return mainPane;
    }

    /**
     * Draws everything on <code>this.canvas</code>.
     * @param fxGraphics2D  object that draws on <code>this.canvas</code>
     */
    private void draw(FXGraphics2D fxGraphics2D) {
        fxGraphics2D.setTransform(new AffineTransform());
        fxGraphics2D.setBackground(Color.white);
        fxGraphics2D.clearRect(0, 0, (int) canvas.getWidth(), (int) canvas.getHeight());


        fxGraphics2D.setTransform(this.cameraTransform);
        fxGraphics2D.translate(-this.startX, -this.startY);

        this.tileMap.draw(fxGraphics2D);

        for (NPC npc : npcList){
            npc.draw(fxGraphics2D);
        }
    }

    /**
     * Updates all the items, cals {@link #draw(FXGraphics2D)} when done
     * @param deltaTime  The time it took between last update call (FPS = 1/deltaTime)
     */
    public void update(Double deltaTime) {
//        System.out.println(1/deltaTime);
        for (NPC npc : npcList){
            npc.update(npcList);
        }
    }

    /**
     * Handles the event when the user scrolls.
     * @param mouseEvent  The MouseEvent that was used to scroll
     */
    //@Todo: needs implementation
    private void onScrolled(MouseEvent mouseEvent) {
        //klinkt moeilijk
    }

    /**
     * Handles the event of a keyboard key pressed by the user.
     * <p>
     * When WASD or Arrow-keys are pressed the method will move the screen in
     * the corresponding direction.
     * @param keyEvent  The KeyEvent that detected the users keyboardpress
     */
    private void onWASD(KeyEvent keyEvent) {
        double verticalPixels = 0;
        double horizontalPixels = 0;

        switch (keyEvent.getCode()) {
            case UP:
            case W:
                verticalPixels = CAMERA_SPEED;
                break;
            case LEFT:
            case A:
                horizontalPixels = CAMERA_SPEED;
                break;
            case DOWN:
            case S:
                verticalPixels = -CAMERA_SPEED;
                break;
            case RIGHT:
            case D:
                horizontalPixels = -CAMERA_SPEED;
                break;
        }

        if(cameraInBounds(horizontalPixels, verticalPixels)) {
            this.cameraTransform.translate(horizontalPixels, verticalPixels);
        }
    }

    private Point2D dragPoint = null;

    /**
     * Handles the event when the user presses the mouse-button
     * <p>
     * If the primary button is pressed <code>this.dragPoint</code> will be set to the
     * position the mouse has at that time.
     * @param mouseEvent  The MouseEvent the user used to click
     */
    private void onMousePressed(MouseEvent mouseEvent) {
        dragPoint = new Point2D.Double(mouseEvent.getX(), mouseEvent.getY());

        moveToPoint(dragPoint);
    }

    /**
     * Handles the event when the user releases the mouse-button
     * <p>
     * If the primary button is released <code>this.dragPoint</code> will be reset to null
     * @param mouseEvent  The MouseEvent the user used to click
     */
    private void onMouseReleased(MouseEvent mouseEvent) {
        dragPoint = null;
    }

    /**
     * Handles the event when the user drags the mouse-button across the screen
     * <p>
     * If the primary button is dragged the method wil check if the operation is allowed by calling {@link #cameraInBounds(double, double)}
     * If correct the screen will be moved to the new position. When done <code>this.dragPoint</code> will be reset to null
     * @param mouseEvent  The MouseEvent the user used to drag
     */
    private void onMouseDragged(MouseEvent mouseEvent) {
        if (this.dragPoint != null) {
            double horizontalPixels = (mouseEvent.getX() - dragPoint.getX());
            double verticalPixels = (mouseEvent.getY() - dragPoint.getY());

            if (this.cameraInBounds(horizontalPixels, verticalPixels)) {
                this.cameraTransform.translate(horizontalPixels, verticalPixels);
            }

            this.dragPoint = new Point2D.Double(mouseEvent.getX(), mouseEvent.getY());
        }
    }

    /**
     * Moves the screen to the centre of the proposed point.
     * @param point  The proposed point to move to
     */
    public void moveToPoint(Point2D point) {
//        this.cameraTransform.translate(point.getX() - (this.cameraTransform.getTranslateX() + this.tileMap.getMapWidth()* this.tileMap.getTileWidth()/ 2), point.getY() - (this.cameraTransform.getTranslateY() + this.tileMap.getMapHeight() * this.tileMap.getTileHeight() /2));
//        this.cameraTransform.translate(
//                point.getX() - this.cameraTransform.getTranslateX() + this.simulatorModule.getStage().getWidth() / 2,
//                point.getY() - this.cameraTransform.getTranslateY() + this.simulatorModule.getStage().getHeight() / 2
//        );
        for (NPC npc : npcList){
            npc.setTarget(point);
        }
    }

    /**
     * Calculates if the given translate will fit within the set bounds.
     * <p>
     * Currently only works on translations, scale not yet implemented.
     * @return  true if the given translate is in bounds
     */
    private boolean cameraInBounds(double translateX, double translateY) {
        return (this.cameraTransform.getTranslateX() + translateX <= 1 &&
                this.cameraTransform.getTranslateX() + translateX >= -(this.endX - this.startX - this.canvas.getWidth()) &&
                this.cameraTransform.getTranslateY() + translateY <= 1 &&
                this.cameraTransform.getTranslateY() + translateY >= -(this.endY - this.startY - this.canvas.getHeight())
        );
    }
}
