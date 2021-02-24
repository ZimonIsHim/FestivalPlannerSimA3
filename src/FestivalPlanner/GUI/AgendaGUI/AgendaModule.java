package FestivalPlanner.GUI.AgendaGUI;

import FestivalPlanner.Agenda.*;

import java.awt.geom.*;
import java.util.ResourceBundle;

import FestivalPlanner.GUI.AbstractGUI;
import FestivalPlanner.GUI.AgendaGUI.PopUpGUI.AboutPopUp;
import FestivalPlanner.GUI.AgendaGUI.PopUpGUI.ArtistPopUp;
import FestivalPlanner.GUI.AgendaGUI.PopUpGUI.EmptyPopUp;
import FestivalPlanner.GUI.AgendaGUI.PopUpGUI.PodiumPopup;
import FestivalPlanner.GUI.PreferencesGUI;
import FestivalPlanner.Util.LanguageHandling.LanguageHandler;
import animatefx.animation.JackInTheBox;
import com.sun.istack.internal.Nullable;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Responsible for placing everything in the correct place in the GUI and making sure all the buttons work.
 */
public class AgendaModule extends AbstractGUI {

    private Stage stage;

    //LanguageHandling
    private ResourceBundle messages = LanguageHandler.getMessages();

    // Agenda variables
    private Agenda agenda = new Agenda();
    private ArtistManager artistManager = new ArtistManager();
    private PodiumManager podiumManager = new PodiumManager();
    private Show currentShow = null;

    // Panes
    private BorderPane mainLayoutPane = new BorderPane();

    // MenuBar
    private MenuBar menuBar = new MenuBar();
    //FileMenu
    private Menu fileMenu = new Menu(messages.getString("file"));
    private MenuItem loadAgendaMenuItem = new MenuItem(messages.getString("load"));
    private MenuItem saveAgendaMenuItem = new MenuItem(messages.getString("save"));
    private MenuItem exitMenuItem = new MenuItem(messages.getString("exit"));
    //EditMenu
    private Menu editMenu = new Menu(messages.getString("edit"));
    private MenuItem editCurrentlySelectedShow = new MenuItem(messages.getString("edit_show"));
    private MenuItem editArtistsAndPodiumsMenuItem = new MenuItem(messages.getString("edit_artists_and_podiums"));
    private MenuItem preferencesMenuItem = new MenuItem(messages.getString("preferences"));
    //HelpMenu
    private Menu helpMenu = new Menu(messages.getString("help"));
    private MenuItem helpGuideMenuItem = new MenuItem(messages.getString("help_guide"));
    private MenuItem javaDocMenuItem = new MenuItem(messages.getString("javadoc"));
    private MenuItem aboutMenuItem = new MenuItem(messages.getString("about"));

    // Layout components
    private AgendaCanvas agendaCanvas;

    //ContextMenu
    private ContextMenu contextMenu = new ContextMenu();
    private MenuItem swapContextItem = new MenuItem(messages.getString("swap")); //TODO: Allow multiple items to be selected. Only swap if 2 are selected.
    private MenuItem editContextItem = new MenuItem(messages.getString("edit"));
    private MenuItem removeContextItem = new MenuItem(messages.getString("remove"));


    /**
     * Constructor of <code>AgendaModule</code>.
     * <p>
     * The given <a href="https://docs.oracle.com/javase/8/javafx/api/javafx/stage/Stage.html">Podium</a> will be stored
     * as a parameter so this stage can be referenced as the main
     * <a href="https://docs.oracle.com/javase/8/javafx/api/javafx/stage/Stage.html">Podium</a>.
     * </p>
     *
     * @param stage will be stored
     *              as a parameter so this stage can be referenced as the owner of the sub stages
     *              <a href="https://docs.oracle.com/javase/8/javafx/api/javafx/stage/Stage.html">Podium</a>
     */
    public AgendaModule(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void load() {
        //Setup methods.
        setup();
        actionHandlingSetup();

        //Stage Settings.
        stage.setScene(new Scene(this.mainLayoutPane));
        stage.setTitle(messages.getString("agenda"));
        stage.setMaximized(true);
        stage.show();
        //Play animation AFTER the show() method has been called, since the animation would
        //otherwise ot be fully visible for the user.
        playAnimation();
    }

    @Override
    public void setup() {
        //Initialise values.
        this.agendaCanvas = new AgendaCanvas(this.agenda);

        //Adding all the children.
        //MenuBar
        fileMenu.getItems().addAll(loadAgendaMenuItem, saveAgendaMenuItem, new SeparatorMenuItem(), exitMenuItem);
        editMenu.getItems().addAll(editArtistsAndPodiumsMenuItem, new SeparatorMenuItem(), editCurrentlySelectedShow, new SeparatorMenuItem(), preferencesMenuItem);
        helpMenu.getItems().addAll(helpGuideMenuItem, javaDocMenuItem, aboutMenuItem);
        menuBar.getMenus().addAll(fileMenu, editMenu, helpMenu);
        //ContextMenu
        contextMenu.getItems().addAll(swapContextItem, editContextItem, new SeparatorMenuItem(), removeContextItem);

        //Adding it all together.
        this.mainLayoutPane.setTop(this.menuBar);
        this.mainLayoutPane.setCenter(this.agendaCanvas.getMainPane());
    }

    @Override
    public void actionHandlingSetup() {
        //Generic
        this.stage.setOnCloseRequest(e -> { //When the main window is closed -> Close the entire program.
            Platform.exit();
        });

        //Canvas
        this.agendaCanvas.getCanvas().setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) { //You can only select with a left-click.
                //In case it still shows it should be hidden, since a new item is selected and some
                //actions executed by the ContextMenu depend on selected items.
                contextMenu.hide();
                onPrimaryButton(e);
            } else if (e.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(agendaCanvas.getMainPane(), e.getScreenX(), e.getScreenY());
            }
        });

        //MenuBar
        //FileMenu
        loadAgendaMenuItem.setOnAction(e -> {
            loadAgenda();
        });

        saveAgendaMenuItem.setOnAction(e -> {
            saveAgenda();
        });

        exitMenuItem.setOnAction(e -> {
            EmptyPopUp emptyPopUp = new EmptyPopUp();
            emptyPopUp.showExitConfirmationPopUp();
        });
        //EditMenu
        editArtistsAndPodiumsMenuItem.setOnAction(e -> {
            ArtistAndPodiumEditorGUI artistAndPodiumEditorGUI = new ArtistAndPodiumEditorGUI(this);
            artistAndPodiumEditorGUI.load();
        });

        editCurrentlySelectedShow.setOnAction(e -> {
            ShowEditorGUI showEditorGUI = new ShowEditorGUI(this);
            if (!this.artistManager.getAllArtistNames().isEmpty() && !this.podiumManager.getAllPodiumNames().isEmpty()) {
                showEditorGUI.load();
            } else {
                showEditorGUI.showNoArtistsOrPodiumsPopUp();
            }
        });

        preferencesMenuItem.setOnAction(e -> {
            PreferencesGUI preferencesGUI = new PreferencesGUI(this.stage);
            preferencesGUI.load();
        });
        //HelpMenu
        aboutMenuItem.setOnAction(e -> {
            AboutPopUp aboutPopUp = new AboutPopUp(this.stage);
            aboutPopUp.load();
        });

        //ContextMenu
        //Edit
        editContextItem.setOnAction(e -> {
            ShowEditorGUI showEditorGUI = new ShowEditorGUI(this);
            if (this.currentShow != null) {
                showEditorGUI.load();
            } else {
                showEditorGUI.showNoLayerSelectedPopUp();
            }
        });
    }

    /**
     * Plays a simple
     * <a href="https://javadoc.io/doc/io.github.typhon0/AnimateFX/latest/animatefx/animation/JackInTheBox.html">JackInTheBox</a>
     * animation after all other methods in {@link #load()} have been called.
     */
    private void playAnimation() {
        new JackInTheBox(this.mainLayoutPane).play();
    }

    /**
     * CallBack method to open <code>this.artistPopup</code>.
     */
    public void artistPopupCallBack() {
        ArtistPopUp artistPopUp = new ArtistPopUp(this.stage, this.artistManager);
        artistPopUp.load();
    }

    /**
     * CallBack method to open <code>this.podiumCallBack</code>.
     */
    public void podiumPopupCallBack() {
        PodiumPopup podiumPopup = new PodiumPopup(this.stage, this.podiumManager);
        podiumPopup.load();
    }

    @Nullable
    private String getLoadAgendaPath() {
        FileChooser fileChooser = new FileChooser();
        try {
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Agenda File", "*.dat"));
            return fileChooser.showOpenDialog(new Stage()).getAbsolutePath();
        } catch (NullPointerException e) {
            EmptyPopUp emptyPopUp = new EmptyPopUp();
            emptyPopUp.showExceptionPopUp(e);
        }
        return null;
    }

    private void loadAgenda() {
        SaveHandler saveHandler = new SaveHandler();
        this.agenda = saveHandler.readAgendaFromFile(getLoadAgendaPath());
        this.agendaCanvas.setAgenda(this.agenda);
        this.currentShow = null;

        //Update podiumManager and ArtistManager.
        for (Show show : this.agenda.getShows()) {
            if (show != null && show.getPodium() != null) {
                if (!this.podiumManager.containsPodium(show.getPodium().getName()))
                    this.podiumManager.addPodium(show.getPodium());
            }

            if (show != null && show.getArtists() != null) {
                for (Artist artist : show.getArtists()) {
                    if (artist != null && !this.artistManager.containsArtist(artist.getName()))
                        this.artistManager.addArtist(artist);
                }
            }
        }
    }

    private void saveAgenda() {
        FileChooser fileChooser = new FileChooser();
        SaveHandler saveHandler = new SaveHandler();
        try {
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Agenda File", "*.dat"));
            String path = fileChooser.showSaveDialog(new Stage()).getAbsolutePath();
            saveHandler.writeAgendaToFile(path, this.agenda);
        } catch (NullPointerException e) {
            EmptyPopUp emptyPopUp = new EmptyPopUp();
            emptyPopUp.showExceptionPopUp(e);
        }
    }

    /**
     * Handles the selecting of <a href="{@docRoot}/FestivalPlanner/AgendaGUI/ShowRectangle2D.html">Rectangle2D</a> in
     * <code>this.agendaCanvas</code>.
     *
     * @param e MouseEvent that was set on the mouseButtonClick
     */
    private void onPrimaryButton(MouseEvent e) {
        Show selectedShow = this.agendaCanvas.showAtPoint(new Point2D.Double(e.getX(), e.getY()));
        if (selectedShow != null) {
            //Reset old show.
            if (this.currentShow != null)
                this.agendaCanvas.rectangleOnShow(this.currentShow).setColor(java.awt.Color.getHSBColor(190 / 360f, .7f, .9f));

            //Starting on new selected.
            this.currentShow = selectedShow;

            //Setting correct color
            this.agendaCanvas.rectangleOnShow(this.currentShow).setColor(java.awt.Color.getHSBColor(100 / 360f, .7f, .7f));
            this.agendaCanvas.reDrawCanvas();

        } else {
            if (this.currentShow != null) {
                this.agendaCanvas.rectangleOnShow(this.currentShow).setColor(java.awt.Color.getHSBColor(190 / 360f, .7f, .9f));
                this.agendaCanvas.reDrawCanvas();
                this.currentShow = null;
            }
        }
    }

    /**
     * Returns the <code>this.currentShow</code> attribute.
     *
     * @return this.currentShow
     */
    @Nullable
    public Show getCurrentShow() {
        return this.currentShow;
    }

    /**
     * Sets the <code>this.currentShow</code> attribute to the parameter's value.
     *
     * @return this.currentShow
     */
    public void setCurrentShow(Show show) {
        this.currentShow = show;

        if (show != null) {
            if (this.agenda.getShows().contains(show)) {
                this.agenda.getShows().remove(show);
            }
            this.agenda.addShow(show);
            this.agendaCanvas.reBuildAgendaCanvas();
        }
    }

    public Agenda getAgenda() {
        return this.agenda;
    }

    public ArtistManager getArtistManager() {
        return this.artistManager;
    }

    public PodiumManager getPodiumManager() {
        return this.podiumManager;
    }


}
