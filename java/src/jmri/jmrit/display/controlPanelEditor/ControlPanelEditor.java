package jmri.jmrit.display.controlPanelEditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import javax.annotation.Nonnull;
import javax.swing.*;

import jmri.CatalogTreeManager;
import jmri.ConfigureManager;
import jmri.InstanceManager;
import jmri.jmrit.catalog.ImageIndexEditor;
import jmri.jmrit.catalog.NamedIcon;
import jmri.jmrit.display.CoordinateEdit;
import jmri.jmrit.display.Editor;
import jmri.jmrit.display.IndicatorTrack;
import jmri.jmrit.display.LinkingObject;
import jmri.jmrit.display.LocoIcon;
import jmri.jmrit.display.MemoryIcon;
import jmri.jmrit.display.Positionable;
import jmri.jmrit.display.PositionableIcon;
import jmri.jmrit.display.PositionableJComponent;
import jmri.jmrit.display.PositionableJPanel;
import jmri.jmrit.display.PositionableLabel;
import jmri.jmrit.display.PositionablePopupUtil;
import jmri.jmrit.display.ReporterIcon;
import jmri.jmrit.display.RpsPositionIcon;
import jmri.jmrit.display.ToolTip;
import jmri.jmrit.display.controlPanelEditor.shape.ShapeDrawer;
import jmri.jmrit.display.palette.ColorDialog;
import jmri.jmrit.display.palette.ItemPalette;
//import jmri.jmrit.display.palette.DecoratorPanel.AJSpinner;
import jmri.jmrit.logix.WarrantTableAction;
import jmri.util.HelpUtil;
import jmri.util.SystemType;
import jmri.util.gui.GuiLafPreferencesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a simple editor for adding jmri.jmrit.display items to a captive
 * JFrame.
 * <p>
 * GUI is structured as a band of common parameters across the top, then a
 * series of things you can add.
 * <p>
 * All created objects are put specific levels depending on their type (higher
 * levels are in front):
 * <ul>
 * <li>BKG background
 * <li>ICONS icons and other drawing symbols
 * <li>LABELS text labels
 * <li>TURNOUTS turnouts and other variable track items
 * <li>SENSORS sensors and other independently modified objects
 * </ul>
 * Note that higher numbers appear behind lower numbers.
 * <p>
 * The "contents" List keeps track of all the objects added to the target frame
 * for later manipulation. Extends the behavior it shares with PanelPro DnD
 * implemented at JDK 1.2 for backward compatibility
 *
 * @author Pete Cressman Copyright: Copyright (c) 2009, 2010, 2011
 */
public class ControlPanelEditor extends Editor implements DropTargetListener, ClipboardOwner {

    protected JMenuBar _menuBar;
    private JMenu _editorMenu;
    protected JMenu _editMenu;
    protected JMenu _fileMenu;
    protected JMenu _optionMenu;
    protected JMenu _iconMenu;
    protected JMenu _zoomMenu;
    private JMenu _markerMenu;
    private JMenu _warrantMenu;
    private JMenu _circuitMenu;
    private JMenu _drawMenu;
    private CircuitBuilder _circuitBuilder;
    private final ArrayList<Rectangle> _highlightGroup = new ArrayList<>();
    private ShapeDrawer _shapeDrawer;
    private ItemPalette _itemPalette;
    private boolean _disableShapeSelection;
    private boolean _disablePortalSelection = true;  // only select PortalIcon in CircuitBuilder
    private String _portalIconFamily = "Standard"; // initial default, must match xml, updated in setIconFamilysetPortalIconFamily
    private HashMap<String, NamedIcon> _portalIconMap;

    private final JCheckBoxMenuItem useGlobalFlagBox = new JCheckBoxMenuItem(Bundle.getMessage("CheckBoxGlobalFlags"));
    private final JCheckBoxMenuItem positionableBox = new JCheckBoxMenuItem(Bundle.getMessage("CheckBoxPositionable"));
    private final JCheckBoxMenuItem controllingBox = new JCheckBoxMenuItem(Bundle.getMessage("CheckBoxControlling"));
    private final JCheckBoxMenuItem showTooltipBox = new JCheckBoxMenuItem(Bundle.getMessage("CheckBoxShowTooltips"));
    private final JCheckBoxMenuItem hiddenBox = new JCheckBoxMenuItem(Bundle.getMessage("CheckBoxHidden"));
    private final JCheckBoxMenuItem disableShapeSelect = new JCheckBoxMenuItem(Bundle.getMessage("disableShapeSelect"));
    private final JRadioButtonMenuItem scrollBoth = new JRadioButtonMenuItem(Bundle.getMessage("ScrollBoth"));
    private final JRadioButtonMenuItem scrollNone = new JRadioButtonMenuItem(Bundle.getMessage("ScrollNone"));
    private final JRadioButtonMenuItem scrollHorizontal = new JRadioButtonMenuItem(Bundle.getMessage("ScrollHorizontal"));
    private final JRadioButtonMenuItem scrollVertical = new JRadioButtonMenuItem(Bundle.getMessage("ScrollVertical"));

    public ControlPanelEditor() {
    }

    public ControlPanelEditor(String name) {
        super(name);
        init(name);
    }

    @Override
    protected void init(String name) {
        setVisible(false);
        java.awt.Container contentPane = this.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        // make menus
        setGlobalSetsLocalFlag(false);
        setUseGlobalFlag(false);
        _menuBar = new JMenuBar();
        _circuitBuilder = new CircuitBuilder(this);
        _shapeDrawer = new ShapeDrawer(this);
        makeDrawMenu();
        makeWarrantMenu(true, true);
        makeIconMenu();
        makeZoomMenu();
        makeMarkerMenu();
        makeOptionMenu();
        makeEditMenu();
        makeFileMenu();

        setJMenuBar(_menuBar);
        addHelpMenu("package.jmri.jmrit.display.ControlPanelEditor", true);

        super.setTargetPanel(null, null);
        super.setTargetPanelSize(300, 300);
        makeDataFlavors();

        // set scrollbar initial state
        setScroll(SCROLL_BOTH);
        scrollBoth.setSelected(true);
        super.setDefaultToolTip(new ToolTip(null, 0, 0, new Font("Serif", Font.PLAIN, 12),
                Color.black, new Color(255, 250, 210), Color.black));
        // register the resulting panel for later configuration
        ConfigureManager cm = InstanceManager.getNullableDefault(jmri.ConfigureManager.class);
        if (cm != null) {
            cm.registerUser(this);
        }
        pack();
        setVisible(true);
    }

    protected void makeIconMenu() {
        _iconMenu = new JMenu(Bundle.getMessage("MenuIcon"));
        _menuBar.add(_iconMenu, 0);

        JMenuItem mi = new JMenuItem(Bundle.getMessage("CircuitBuilder"));
        mi.addActionListener((ActionEvent event) -> _circuitBuilder.openCBWindow());
        setMenuAcceleratorKey(mi, KeyEvent.VK_B);
        _iconMenu.add(mi);

        _iconMenu.add(new JSeparator()); // below are different types of tables

        mi = new JMenuItem(Bundle.getMessage("MenuItemItemPalette"));
        mi.addActionListener(new ActionListener() {
            Editor editor;

            ActionListener init(Editor ed) {
                editor = ed;
                return this;
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                _itemPalette = ItemPalette.getDefault(Bundle.getMessage("MenuItemItemPalette"), editor);
                assert _itemPalette != null;
                _itemPalette.setVisible(true);
            }
        }.init(this));
        setMenuAcceleratorKey(mi, KeyEvent.VK_P);
        _iconMenu.add(mi);

        _iconMenu.add(new jmri.jmrit.beantable.OBlockTableAction(Bundle.getMessage("MenuItemOBlockTable")));
        mi = (JMenuItem) _iconMenu.getMenuComponent(3);
        setMenuAcceleratorKey(mi, KeyEvent.VK_O);

        _iconMenu.add(new jmri.jmrit.beantable.ListedTableAction(Bundle.getMessage("MenuItemTableList")));
        mi = (JMenuItem) _iconMenu.getMenuComponent(4);
        setMenuAcceleratorKey(mi, KeyEvent.VK_T);
    }

    @SuppressWarnings("deprecation")  // META_MASK CTRL_MASK
    private void setMenuAcceleratorKey (JMenuItem mi,  int key) {
        if (SystemType.isMacOSX()) {
            mi.setAccelerator(KeyStroke.getKeyStroke(key, InputEvent.META_MASK));
        } else {
            mi.setAccelerator(KeyStroke.getKeyStroke(key, InputEvent.CTRL_MASK));
        }
    }

    protected void makeCircuitMenu(boolean edit) {
        if (edit) {
            if (_circuitMenu == null) {
                ItemPalette.loadIcons();
                _circuitMenu = _circuitBuilder.makeMenu();
                int idx = _menuBar.getComponentIndex(_warrantMenu);
                _menuBar.add(_circuitMenu, ++idx);
                _menuBar.revalidate();
            }
        } else if (_circuitMenu != null) {
            _circuitBuilder.closeCBWindow();
            _circuitMenu = null;
        }
    }

    protected void makeDrawMenu() {
        if (_drawMenu == null) {
            _drawMenu = _shapeDrawer.makeMenu();
            _drawMenu.add(disableShapeSelect);
            disableShapeSelect.addActionListener((ActionEvent event) -> _disableShapeSelection = disableShapeSelect.isSelected());
        }
        _menuBar.add(_drawMenu, 0);
    }

    public boolean getShapeSelect() {
        return !_disableShapeSelection;
    }

    public void setShapeSelect(boolean set) {
        _disableShapeSelection = !set;
        disableShapeSelect.setSelected(_disableShapeSelection);
    }

    public String getPortalIconFamily() {
        return _portalIconFamily;
    }

    public void setPortalIconFamily(String family) {
        if (family != null && !family.equals(_portalIconFamily)) {
            _portalIconMap = null;
        }
        _portalIconFamily = family;
    }

    @Nonnull
    public HashMap<String, NamedIcon> getPortalIconMap() {
        if (_portalIconMap == null) {
            ItemPalette.loadIcons();
            _portalIconMap = ItemPalette.getIconMap("Portal", _portalIconFamily);
            if (_portalIconMap == null) {
                HashMap<String, HashMap<String, NamedIcon>> familyMap = ItemPalette.getFamilyMaps("Portal");
                _portalIconMap = familyMap.get("Standard");
                // fill in the default PortalIconMap for CPE if it was null up to now.
                // TODO check if this is ever called since we fixed getting it from ItemPalette above, remove for better maintainability
                if (_portalIconMap == null) {
                    log.warn("empty PortalIconMap returned");
                    _portalIconMap = new HashMap<>();
                    _portalIconMap.put(PortalIcon.HIDDEN,
                            new NamedIcon("resources/icons/Invisible.gif", "resources/icons/Invisible.gif"));
                    _portalIconMap.put(PortalIcon.PATH,
                            new NamedIcon("resources/icons/greenSquare.gif", "resources/icons/greenSquare.gif"));
                    _portalIconMap.put(PortalIcon.VISIBLE,
                            new NamedIcon("resources/icons/throttles/RoundRedCircle20.png", "resources/icons/throttles/RoundRedCircle20.png"));
                    _portalIconMap.put(PortalIcon.TO_ARROW,
                            new NamedIcon("resources/icons/track/toArrow.gif", "resources/icons/track/toArrow.gif"));
                    _portalIconMap.put(PortalIcon.FROM_ARROW,
                            new NamedIcon("resources/icons/track/fromArrow.gif", "resources/icons/track/fromArrow.gif"));
                }
            }
        }
        // return a copy, not the ItemPalette's map!
        return PositionableIcon.cloneMap(_portalIconMap, null);
    }

    public ShapeDrawer getShapeDrawer() {
        return _shapeDrawer;
    }

    protected void makeZoomMenu() {
        _zoomMenu = new JMenu(Bundle.getMessage("MenuZoom"));
        _menuBar.add(_zoomMenu, 0);
        JMenuItem addItem = new JMenuItem(Bundle.getMessage("NoZoom"));
        _zoomMenu.add(addItem);
        addItem.addActionListener((ActionEvent event) -> zoomRestore());

        addItem = new JMenuItem(Bundle.getMessage("Zoom", "..."));
        _zoomMenu.add(addItem);
        PositionableJComponent z = new PositionableJComponent(this);
        z.setScale(getPaintScale());
        addItem.addActionListener(CoordinateEdit.getZoomEditAction(z));

        addItem = new JMenuItem(Bundle.getMessage("ZoomFit"));
        _zoomMenu.add(addItem);
        addItem.addActionListener((ActionEvent event) -> zoomToFit());
    }

    protected void makeWarrantMenu(boolean edit, boolean addMenu) {
        JMenu oldMenu = _warrantMenu;
        _warrantMenu = jmri.jmrit.logix.WarrantTableAction.getDefault().makeWarrantMenu(edit);
        if (_warrantMenu == null) {
            _warrantMenu = new JMenu(ResourceBundle.getBundle("jmri.jmrit.logix.WarrantBundle").getString("MenuWarrant"));
            JMenuItem aboutItem = new JMenuItem(Bundle.getMessage("AboutWarrant"));
            HelpUtil.enableHelpOnButton(aboutItem, "package.jmri.jmrit.logix.Warrant");
            _warrantMenu.add(aboutItem);
            aboutItem = new JMenuItem(Bundle.getMessage("AboutOBlock"));
            HelpUtil.enableHelpOnButton(aboutItem, "package.jmri.jmrit.logix.OBlockTable");
            _warrantMenu.add(aboutItem);
            aboutItem = new JMenuItem(Bundle.getMessage("AboutCircuitMenu"));
            HelpUtil.enableHelpOnButton(aboutItem, "package.jmri.jmrit.display.CircuitBuilder");
            _warrantMenu.add(aboutItem);
            aboutItem.addActionListener((ActionEvent event) -> {
                makeCircuitMenu(true);
//                openCircuitWindow();
            });
        }
        if (edit) {
            makeCircuitMenu(edit);
            JMenuItem item = new JMenuItem(Bundle.getMessage("OpenCircuitMenu"));
            _warrantMenu.add(item);
            item.addActionListener((ActionEvent event) -> _circuitBuilder.openCBWindow());
        }
        if (addMenu) {
            _menuBar.add(_warrantMenu, 0);
        } else if (oldMenu != null) {
            int idx = _menuBar.getComponentIndex(oldMenu);
            _menuBar.remove(oldMenu);
            _menuBar.add(_warrantMenu, idx);

        }
        _menuBar.revalidate();
    }

    protected void makeMarkerMenu() {
        _markerMenu = new JMenu(Bundle.getMessage("MenuMarker"));
        _menuBar.add(_markerMenu);
        _markerMenu.add(new AbstractAction(Bundle.getMessage("AddLoco")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                locoMarkerFromInput();
            }
        });
        _markerMenu.add(new AbstractAction(Bundle.getMessage("AddLocoRoster")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                locoMarkerFromRoster();
            }
        });
        _markerMenu.add(new AbstractAction(Bundle.getMessage("RemoveMarkers")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeMarkers();
            }
        });
    }

    protected void makeOptionMenu() {
        _optionMenu = new JMenu(Bundle.getMessage("MenuOptions"));
        _menuBar.add(_optionMenu, 0);
        // use globals item
        _optionMenu.add(useGlobalFlagBox);
        useGlobalFlagBox.addActionListener((ActionEvent event) -> setUseGlobalFlag(useGlobalFlagBox.isSelected()));
        useGlobalFlagBox.setSelected(useGlobalFlag());
        // positionable item
        _optionMenu.add(positionableBox);
        positionableBox.addActionListener((ActionEvent event) -> setAllPositionable(positionableBox.isSelected()));
        positionableBox.setSelected(allPositionable());
        // controlable item
        _optionMenu.add(controllingBox);
        controllingBox.addActionListener((ActionEvent event) -> setAllControlling(controllingBox.isSelected()));
        controllingBox.setSelected(allControlling());
        // hidden item
        _optionMenu.add(hiddenBox);
        hiddenBox.addActionListener((ActionEvent event) -> setShowHidden(hiddenBox.isSelected()));
        hiddenBox.setSelected(showHidden());

        _optionMenu.add(showTooltipBox);
        showTooltipBox.addActionListener((ActionEvent e) -> setAllShowToolTip(showTooltipBox.isSelected()));
        showTooltipBox.setSelected(showToolTip());

        // Show/Hide Scroll Bars
        JMenu scrollMenu = new JMenu(Bundle.getMessage("ComboBoxScrollable"));
        _optionMenu.add(scrollMenu);
        ButtonGroup scrollGroup = new ButtonGroup();
        scrollGroup.add(scrollBoth);
        scrollMenu.add(scrollBoth);
        scrollBoth.addActionListener((ActionEvent event) -> setScroll(SCROLL_BOTH));
        scrollGroup.add(scrollNone);
        scrollMenu.add(scrollNone);
        scrollNone.addActionListener((ActionEvent event) -> setScroll(SCROLL_NONE));
        scrollGroup.add(scrollHorizontal);
        scrollMenu.add(scrollHorizontal);
        scrollHorizontal.addActionListener((ActionEvent event) -> setScroll(SCROLL_HORIZONTAL));
        scrollGroup.add(scrollVertical);
        scrollMenu.add(scrollVertical);
        scrollVertical.addActionListener((ActionEvent event) -> setScroll(SCROLL_VERTICAL));
    }

    private void makeFileMenu() {
        _fileMenu = new JMenu(Bundle.getMessage("MenuFile"));
        _menuBar.add(_fileMenu, 0);
        _fileMenu.add(new jmri.jmrit.display.NewPanelAction(Bundle.getMessage("MenuItemNew")));

        _fileMenu.add(new jmri.configurexml.StoreXmlUserAction(Bundle.getMessage("FileMenuItemStore")));
        JMenuItem storeIndexItem = new JMenuItem(Bundle.getMessage("MIStoreImageIndex"));
        _fileMenu.add(storeIndexItem);
        storeIndexItem.addActionListener((ActionEvent event) -> InstanceManager.getDefault(CatalogTreeManager.class).storeImageIndex());

        JMenuItem editItem = new JMenuItem(Bundle.getMessage("renamePanelMenu", "..."));
        PositionableJComponent z = new PositionableJComponent(this);
        z.setScale(getPaintScale());
        editItem.addActionListener(CoordinateEdit.getNameEditAction(z));
        _fileMenu.add(editItem);

        editItem = new JMenuItem(Bundle.getMessage("editIndexMenu"));
        _fileMenu.add(editItem);
        editItem.addActionListener((ActionEvent event) -> {
                ImageIndexEditor ii = InstanceManager.getDefault(ImageIndexEditor.class);
                ii.pack();
                ii.setVisible(true);
        });

        editItem = new JMenuItem(Bundle.getMessage("PEView"));
        _fileMenu.add(editItem);
        editItem.addActionListener((ActionEvent event) -> {
            changeView("jmri.jmrit.display.panelEditor.PanelEditor");
            if (_itemPalette != null) {
                _itemPalette.dispose();
            }
        });

        _fileMenu.addSeparator();
        JMenuItem deleteItem = new JMenuItem(Bundle.getMessage("DeletePanel"));
        _fileMenu.add(deleteItem);
        deleteItem.addActionListener((ActionEvent event) -> {
            if (deletePanel()) {
                dispose();
            }
        });
        _fileMenu.addSeparator();
        editItem = new JMenuItem(Bundle.getMessage("CloseEditor"));
        _fileMenu.add(editItem);
        editItem.addActionListener((ActionEvent event) -> setAllEditable(false));
    }

    /**
     * Create an Edit menu to support cut/copy/paste. An incredible hack to get
     * some semblance of CCP between panels. The hack works for one of two
     * problems. 1. Invoking a copy to the system clipboard causes a delayed
     * repaint placed on the EventQueue whenever ScrollBars are invoked. This
     * repaint ends with a null pointer exception at
     * javax.swing.plaf.basic.BasicScrollPaneUI.paint(BasicScrollPaneUI.java:90)
     * This error occurs regardless of the method used to put the copy in the
     * clipboard - JDK 1.2 style or 1.4 TransferHandler Fixed! Get the plaf glue
     * (BasicScrollPaneUI) and call installUI(_panelScrollPane) See
     * copyToClipboard() below, line 527 (something the Java code should have
     * done) No scrollbars - no problem. Hack does not fix this problem. 2. The
     * clipboard provides a shallow copy of what was placed there. For things
     * that have an icon Map (ArrayLists) the Tranferable data is shallow. The
     * Hack to work around this is: Place a reference to the panel copying to
     * the clipboard in the clipboard and let the pasting panel callback to the
     * copying panel to get the data. See public ArrayList&lt;Positionable&gt;
     * getClipGroup() {} below.
     */
    protected void makeEditMenu() {
        _editMenu = new JMenu(Bundle.getMessage("ButtonEdit"));
        _menuBar.add(_editMenu, 0);
        _editMenu.setMnemonic(KeyEvent.VK_E);
        JMenuItem menuItem = new JMenuItem(Bundle.getMessage("MenuItemCut"));
        menuItem.addActionListener((ActionEvent event) -> {
            copyToClipboard();
            removeSelections(null);
        });
        setMenuAcceleratorKey(menuItem, KeyEvent.VK_X);
        _editMenu.add(menuItem);

        menuItem = new JMenuItem(Bundle.getMessage("MenuItemCopy"));
        menuItem.addActionListener((ActionEvent event) -> copyToClipboard());
        setMenuAcceleratorKey(menuItem, KeyEvent.VK_C);
        _editMenu.add(menuItem);

        menuItem = new JMenuItem(Bundle.getMessage("MenuItemPaste"));
        menuItem.addActionListener((ActionEvent event) -> pasteFromClipboard());
        setMenuAcceleratorKey(menuItem, KeyEvent.VK_V);
        _editMenu.add(menuItem);

        _editMenu.add(makeSelectTypeMenu());
        _editMenu.add(makeSelectLevelMenu());

        menuItem = new JMenuItem(Bundle.getMessage("SelectAll"));
        menuItem.addActionListener((ActionEvent event) -> {
            _selectionGroup = new ArrayList<>(getContents());
            _targetPanel.repaint();
        });
        setMenuAcceleratorKey(menuItem, KeyEvent.VK_A);
        _editMenu.add(menuItem);
    }

    private JMenu makeSelectTypeMenu() {
        JMenu menu = new JMenu(Bundle.getMessage("SelectType"));
        ButtonGroup typeGroup = new ButtonGroup();
        // I18N use existing jmri.NamedBeanBundle keys
        JRadioButtonMenuItem button = makeSelectTypeButton("IndicatorTrack", "jmri.jmrit.display.IndicatorTrackIcon");
        typeGroup.add(button);
        menu.add(button);
        button = makeSelectTypeButton("IndicatorTO", "jmri.jmrit.display.IndicatorTurnoutIcon");
        typeGroup.add(button);
        menu.add(button);
        button = makeSelectTypeButton("BeanNameTurnout", "jmri.jmrit.display.TurnoutIcon");
        typeGroup.add(button);
        menu.add(button);
        button = makeSelectTypeButton("BeanNameSensor", "jmri.jmrit.display.SensorIcon");
        typeGroup.add(button);
        menu.add(button);
        button = makeSelectTypeButton("Shape", "jmri.jmrit.display.controlPanelEditor.shape.PositionableShape");
        typeGroup.add(button);
        menu.add(button);
        button = makeSelectTypeButton("BeanNameSignalMast", "jmri.jmrit.display.SignalMastIcon");
        typeGroup.add(button);
        menu.add(button);
        button = makeSelectTypeButton("BeanNameSignalHead", "jmri.jmrit.display.SignalHeadIcon");
        typeGroup.add(button);
        menu.add(button);
        button = makeSelectTypeButton("BeanNameMemory", "jmri.jmrit.display.MemoryIcon");
        typeGroup.add(button);
        menu.add(button);
        button = makeSelectTypeButton("MemoryInput", "jmri.jmrit.display.PositionableJPanel");
        typeGroup.add(button);
        menu.add(button);
        button = makeSelectTypeButton("MultiSensor", "jmri.jmrit.display.MultiSensorIcon");
        typeGroup.add(button);
        menu.add(button);
        button = makeSelectTypeButton("LocoID", "jmri.jmrit.display.LocoIcon");
        typeGroup.add(button);
        menu.add(button);
        button = makeSelectTypeButton("BeanNameLight", "jmri.jmrit.display.LightIcon");
        typeGroup.add(button);
        menu.add(button);
        return menu;
    }

    private JRadioButtonMenuItem makeSelectTypeButton(String label, String className) {
        JRadioButtonMenuItem button = new JRadioButtonMenuItem(Bundle.getMessage(label));
        button.addActionListener(new ActionListener() {
            String cName;

            ActionListener init(String name) {
                cName = name;
                return this;
            }

            @Override
            public void actionPerformed(ActionEvent event) {
                selectType(cName);
            }
        }.init(className));
        return button;
    }

    private void selectType(String name) {
        try {
            Class<?> cl = Class.forName(name);
            _selectionGroup = new ArrayList<>();
            for (Positionable pos : getContents()) {
                if (cl.isInstance(pos)) {
                    _selectionGroup.add(pos);
                }
            }
        } catch (ClassNotFoundException cnfe) {
            log.error("selectType Menu {}", cnfe.toString());
        }
        _targetPanel.repaint();
    }

    private JMenu makeSelectLevelMenu() {
        JMenu menu = new JMenu(Bundle.getMessage("SelectLevel"));
        ButtonGroup levelGroup = new ButtonGroup();
        JRadioButtonMenuItem button;
        for (int i = 0; i < 11; i++) {
            button = new JRadioButtonMenuItem(Bundle.getMessage("selectLevel", "" + i));
            levelGroup.add(button);
            menu.add(button);
            button.addActionListener(new ActionListener() {
                int j;

                ActionListener init(int k) {
                    j = k;
                    return this;
                }

                @Override
                public void actionPerformed(ActionEvent event) {
                    selectLevel(j);
                }
            }.init(i));
        }
        return menu;
    }

    private void selectLevel(int i) {
        _selectionGroup = new ArrayList<>();
        for (Positionable pos : getContents()) {
            if (pos.getDisplayLevel() == i) {
                _selectionGroup.add(pos);
            }
        }
        _targetPanel.repaint();
    }

    ////////////////////////// end Menus //////////////////////////
    public CircuitBuilder getCircuitBuilder() {
        return _circuitBuilder;
    }

    private void pasteFromClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        DataFlavor[] flavors = clipboard.getAvailableDataFlavors();
        for (DataFlavor flavor : flavors) {
            if (_positionableListDataFlavor.equals(flavor)) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Positionable> clipGroup = (List<Positionable>) clipboard.getData(_positionableListDataFlavor);
                    if (clipGroup != null && clipGroup.size() > 0) {
                        Positionable pos = clipGroup.get(0);
                        int minX = pos.getLocation().x;
                        int minY = pos.getLocation().y;
                        // locate group at mouse point
                        for (int i = 1; i < clipGroup.size(); i++) {
                            pos = clipGroup.get(i);
                            minX = Math.min(minX, pos.getLocation().x);
                            minY = Math.min(minY, pos.getLocation().y);
                        }
                        if (_pastePending) {
                            abortPasteItems();
                        }
                        _selectionGroup = new ArrayList<>();
                        for (Positionable positionable : clipGroup) {
                            pos = positionable;
                            // make positionable belong to this editor
                            pos.setEditor(this);
                            pos.setLocation(pos.getLocation().x + _anchorX - minX, pos.getLocation().y + _anchorY - minY);
                            // now set display level in the pane.
                            pos.setDisplayLevel(pos.getDisplayLevel());
                            try {
                                pos.setId(null);
                                putItem(pos);
                            } catch (Positionable.DuplicateIdException e) {
                                // This should never happen
                                log.error("Editor.putItem() with null id has thrown DuplicateIdException", e);
                            }
                            pos.updateSize();
                            pos.setVisible(true);
                            _selectionGroup.add(pos);
                            if (pos instanceof PositionableIcon) {
                                jmri.NamedBean bean = pos.getNamedBean();
                                if (bean != null) {
                                    ((PositionableIcon) pos).displayState(bean.getState());
                                }
                            } else if (pos instanceof MemoryIcon) {
                                ((MemoryIcon) pos).displayState();
                            } else if (pos instanceof PositionableJComponent) {
                                ((PositionableJComponent) pos).displayState();
                            }
                            log.debug("Paste Added at ({}, {})", pos.getLocation().x, pos.getLocation().y);
                        }
                    }
                    return;
                } catch (IOException ioe) {
                    log.warn("Editor Paste caught IOException", ioe);
                } catch (UnsupportedFlavorException ufe) {
                    log.warn("Editor Paste caught UnsupportedFlavorException", ufe);
                }
            }
        }
    }

    /*
     * The editor instance is dragged.  When dropped this editor will reference
     * the list of positionables (_clipGroup) for pasting
     */
    private void copyToClipboard() {
        if (_selectionGroup != null) {
            ArrayList<Positionable> dragGroup = new ArrayList<>();

            for (Positionable comp : _selectionGroup) {
                Positionable pos = comp.deepClone();
                dragGroup.add(pos);
                removeFromTarget(pos);   // cloned item gets added to _targetPane during cloning
            }
            log.debug("copyToClipboard: cloned _selectionGroup, size= {}", _selectionGroup.size());
            _clipGroup = dragGroup;

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new PositionableListDnD(_clipGroup), this);
            log.debug("copyToClipboard: setContents _selectionGroup, size= {}", _selectionGroup.size());
        } else {
            _clipGroup = null;
        }
    }

    ArrayList<Positionable> _clipGroup;

    public ArrayList<Positionable> getClipGroup() {
        if (log.isDebugEnabled()) { // avoid string concatination if not debug
            log.debug("getClipGroup: _clipGroup{}", _clipGroup == null ? "=null" : ", size= " + _clipGroup.size());
        }
        if (_clipGroup == null) {
            return null;
        }
        ArrayList<Positionable> clipGrp = new ArrayList<>();
        for (Positionable _comp : _clipGroup) {
            Positionable pos = _comp.deepClone();
            clipGrp.add(pos);
            removeFromTarget(pos);   // cloned item gets added to _targetPane during cloning
        }
        return clipGrp;
    }

    ///// implementation of ClipboardOwner
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        /* don't care */
        log.debug("lostOwnership: content flavor[0] = {}", contents.getTransferDataFlavors()[0]);
    }

    @Override
    public void setAllEditable(boolean edit) {
        if (_warrantMenu != null) {
            _menuBar.remove(_warrantMenu);
        }
        if (_circuitMenu != null) {
            _menuBar.remove(_circuitMenu);
            _circuitMenu = null;
        }
        if (edit) {
            if (_editorMenu != null) {
                _menuBar.remove(_editorMenu);
            }
            if (_markerMenu != null) {
                _menuBar.remove(_markerMenu);
            }
            if (_drawMenu == null) {
                makeDrawMenu();
            } else {
                _menuBar.add(_drawMenu, 0);
            }
            makeWarrantMenu(true, true);

            if (_iconMenu == null) {
                makeIconMenu();
            } else {
                _menuBar.add(_iconMenu, 0);
            }
            if (_zoomMenu == null) {
                makeZoomMenu();
            } else {
                _menuBar.add(_zoomMenu, 0);
            }
            if (_optionMenu == null) {
                makeOptionMenu();
            } else {
                _menuBar.add(_optionMenu, 0);
            }
            if (_editMenu == null) {
                makeEditMenu();
            } else {
                _menuBar.add(_editMenu, 0);
            }
            if (_fileMenu == null) {
                makeFileMenu();
            } else {
                _menuBar.add(_fileMenu, 0);
            }
        } else {
            if (_fileMenu != null) {
                _menuBar.remove(_fileMenu);
            }
            if (_editMenu != null) {
                _menuBar.remove(_editMenu);
            }
            if (_optionMenu != null) {
                _menuBar.remove(_optionMenu);
            }
            if (_zoomMenu != null) {
                _menuBar.remove(_zoomMenu);
            }
            if (_iconMenu != null) {
                _menuBar.remove(_iconMenu);
            }
            if (_drawMenu != null) {
                _menuBar.remove(_drawMenu);
            }
            if (InstanceManager.getDefault(jmri.jmrit.logix.OBlockManager.class).getNamedBeanSet().size() > 1) {
                makeWarrantMenu(false, true);
//                _circuitMenu = null;
            }
            if (_markerMenu == null) {
                makeMarkerMenu();
            } else {
                _menuBar.add(_markerMenu, 0);
            }
            if (_editorMenu == null) {  // replaces _fileMenu
                _editorMenu = new JMenu(Bundle.getMessage("MenuEdit"));
                _editorMenu.add(new AbstractAction(Bundle.getMessage("OpenEditor")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setAllEditable(true);
                    }
                });
            }
            _menuBar.add(_editorMenu, 0);
        }
        super.setAllEditable(edit);
        setTitle();
        _menuBar.revalidate();
    }

    @Override
    public void setUseGlobalFlag(boolean set) {
        positionableBox.setEnabled(set);
        controllingBox.setEnabled(set);
        super.setUseGlobalFlag(set);
    }

    private void zoomRestore() {
        List<Positionable> contents = getContents();
        for (Positionable p : contents) {
            p.setLocation(p.getX() + _fitX, p.getY() + _fitY);
        }
        setPaintScale(1.0);
    }

    int _fitX = 0;
    int _fitY = 0;

    private void zoomToFit() {
        double minX = 1000.0;
        double maxX = 0.0;
        double minY = 1000.0;
        double maxY = 0.0;
        List<Positionable> contents = getContents();
        for (Positionable p : contents) {
            minX = Math.min(p.getX(), minX);
            minY = Math.min(p.getY(), minY);
            maxX = Math.max(p.getX() + p.getWidth(), maxX);
            maxY = Math.max(p.getY() + p.getHeight(), maxY);
        }
        _fitX = (int) Math.floor(minX);
        _fitY = (int) Math.floor(minY);

        JFrame frame = getTargetFrame();
        Container contentPane = getTargetFrame().getContentPane();
        Dimension dim = contentPane.getSize();
        Dimension d = getTargetPanel().getSize();
        getTargetPanel().setSize((int) Math.ceil(maxX - minX), (int) Math.ceil(maxY - minY));

        JScrollPane scrollPane = getPanelScrollPane();
        scrollPane.getHorizontalScrollBar().setValue(0);
        scrollPane.getVerticalScrollBar().setValue(0);
        JViewport viewPort = scrollPane.getViewport();
        Dimension dv = viewPort.getExtentSize();

        int dX = frame.getWidth() - dv.width;
        int dY = frame.getHeight() - dv.height;
        log.debug("zoomToFit: layoutWidth= {}, layoutHeight= {}\n\tframeWidth= {}, frameHeight= {}, viewWidth= {}, viewHeight= {}\n\tconWidth= {}, conHeight= {}, panelWidth= {}, panelHeight= {}",
                (maxX - minX), (maxY - minY), frame.getWidth(), frame.getHeight(), dv.width, dv.height, dim.width, dim.height, d.width, d.height);
        double ratioX = dv.width / (maxX - minX);
        double ratioY = dv.height / (maxY - minY);
        double ratio = Math.min(ratioX, ratioY);
        /*
         if (ratioX<ratioY) {
         if (ratioX>1.0) {
         ratio = ratioX;
         } else {
         ratio = ratioY;
         }
         } else {
         if (ratioY<1.0) {
         ratio = ratioX;
         } else {
         ratio = ratioY;
         }
         } */
        _fitX = (int) Math.floor(minX);
        _fitY = (int) Math.floor(minY);
        for (Positionable p : contents) {
            p.setLocation(p.getX() - _fitX, p.getY() - _fitY);
        }
        setScroll(SCROLL_BOTH);
        setPaintScale(ratio);
        setScroll(SCROLL_NONE);
        scrollNone.setSelected(true);
        //getTargetPanel().setSize((int)Math.ceil(maxX), (int)Math.ceil(maxY));
        frame.setSize((int) Math.ceil((maxX - minX) * ratio) + dX, (int) Math.ceil((maxY - minY) * ratio) + dY);
        scrollPane.getHorizontalScrollBar().setValue(0);
        scrollPane.getVerticalScrollBar().setValue(0);
        log.debug("zoomToFit: ratio= {}, w= {}, h= {}, frameWidth= {}, frameHeight= {}",
                ratio, (maxX - minX), (maxY - minY), frame.getWidth(), frame.getHeight());
    }

    @Override
    public void setTitle() {
        String name = getName();
        if (name == null || name.length() == 0) {
            name = Bundle.getMessage("untitled");
        }
        String ending = " " + Bundle.getMessage("LabelEditor");
        String defaultName = Bundle.getMessage("ControlPanelEditor");
        defaultName = defaultName.substring(0, defaultName.length() - ending.length());
        if (name.endsWith(ending)) {
            name = name.substring(0, name.length() - ending.length());
        }
        if (name.equals(defaultName)) {
            name = Bundle.getMessage("untitled") + "(" + name + ")";
        }
       if (isEditable()) {
            super.setTitle(name + " " + Bundle.getMessage("LabelEditor"));
        } else {
            super.setTitle(name);
        }
        setName(name);
    }

    // all content loaded from file.
    public void loadComplete() {
        log.debug("loadComplete");
    }

    /**
     * After construction, initialize all the widgets to their saved config
     * settings.
     */
    @Override
    public void initView() {
        positionableBox.setSelected(allPositionable());
        controllingBox.setSelected(allControlling());
        //showCoordinatesBox.setSelected(showCoordinates());
        showTooltipBox.setSelected(showToolTip());
        hiddenBox.setSelected(showHidden());
        switch (_scrollState) {
            case SCROLL_NONE:
                scrollNone.setSelected(true);
                break;
            case SCROLL_BOTH:
                scrollBoth.setSelected(true);
                break;
            case SCROLL_HORIZONTAL:
                scrollHorizontal.setSelected(true);
                break;
            case SCROLL_VERTICAL:
                scrollVertical.setSelected(true);
                break;
            default:
                log.warn("Unhandled scroll state: {}", _scrollState);
                break;
        }
        log.debug("InitView done");
    }

    ////////////////// Overridden methods of Editor //////////////////
    private boolean _manualSelection = false;

    @Override
    public void deselectSelectionGroup() {
        _circuitBuilder.hidePortalIcons(false);
        super.deselectSelectionGroup();
    }

    protected Positionable getCurrentSelection(MouseEvent event) {
        if (_pastePending && !event.isPopupTrigger() && !event.isMetaDown() && !event.isAltDown()) {
            return getCopySelection(event);
        }
        List<Positionable> selections = getSelectedItems(event);
        if (_disableShapeSelection || _disablePortalSelection) {
            ArrayList<Positionable> list = new ArrayList<>();
            for (Positionable pos : selections) {
                if (_disableShapeSelection && pos instanceof jmri.jmrit.display.controlPanelEditor.shape.PositionableShape) {
                    continue;
                }
                if (_disablePortalSelection && pos instanceof PortalIcon) {
                    continue;
                }
                list.add(pos);
            }
            selections = list;
        }
        Positionable selection = null;
        if (selections.size() > 0) {
            if (event.isControlDown()) {
                if (event.isShiftDown() && selections.size() > 3) {
                    if (_manualSelection) {
                        // selection made - don't change it
                        deselectSelectionGroup();
                        return _currentSelection;
                    }
                    // show list
                    String[] selects = new String[selections.size()];
                    Iterator<Positionable> iter = selections.iterator();
                    int i = 0;
                    while (iter.hasNext()) {
                        Positionable pos = iter.next();
                        if (pos instanceof jmri.NamedBean) {
                            selects[i++] = ((jmri.NamedBean) pos).getDisplayName();
                        } else {
                            selects[i++] = pos.getNameString();
                        }
                    }
                    Object select = JOptionPane.showInputDialog(this, Bundle.getMessage("multipleSelections"),
                            Bundle.getMessage("QuestionTitle"), JOptionPane.QUESTION_MESSAGE,
                            null, selects, null);
                    if (select != null) {
                        iter = selections.iterator();
                        while (iter.hasNext()) {
                            Positionable pos = iter.next();
                            String name;
                            if (pos instanceof jmri.NamedBean) {
                                name = ((jmri.NamedBean) pos).getDisplayName();
                            } else {
                                name = pos.getNameString();
                            }
                            if (select.equals(name)) {
                                _manualSelection = true;
                                highlight(pos);
                                return pos;
                            }
                        }
                    } else {
                        selection = selections.get(selections.size() - 1);
                    }
                } else {
                    // select bottom-most item over the background, otherwise take the background item
                    selection = selections.get(selections.size() - 1);
                    if (selection.getDisplayLevel() <= BKG && selections.size() > 1) {
                        selection = selections.get(selections.size() - 2);
                    }
//              _manualSelection = false;
                }
            } else {
                if (event.isShiftDown() && selections.size() > 1) {
                    selection = selections.get(1);
                } else {
                    selection = selections.get(0);
                }
                if (selection.getDisplayLevel() <= BKG) {
                    selection = null;
                }
                _manualSelection = false;
            }
        } else {
            if (event.isControlDown() && (event.isPopupTrigger() || event.isMetaDown() || event.isAltDown())) {
                ActionListener ca;
                Editor ed = this;
                ca = e -> {
                    if (_itemPalette != null) {
                        _itemPalette.setEditor(ed);
                    }
                };
                new ColorDialog(this, getTargetPanel(), ColorDialog.ONLY, ca);
            }
        }
        if (!isEditable() && selection != null && selection.isHidden()) {
            selection = null;
        }
        return selection;
    }

    private Positionable getCopySelection(MouseEvent event) {
        if (_selectionGroup == null) {
            return null;
        }
        double x = event.getX();
        double y = event.getY();

        for (Positionable p : _selectionGroup) {
            Rectangle2D.Double rect2D = new Rectangle2D.Double(p.getX() * _paintScale,
                    p.getY() * _paintScale,
                    p.maxWidth() * _paintScale,
                    p.maxHeight() * _paintScale);
            if (rect2D.contains(x, y)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Capture key events.
     *
     * @param e the event
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int x = 0;
        int y = 0;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_KP_UP:
            case KeyEvent.VK_NUMPAD8:
                y = -1;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_KP_DOWN:
            case KeyEvent.VK_NUMPAD2:
                y = 1;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_KP_LEFT:
            case KeyEvent.VK_NUMPAD4:
                x = -1;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_KP_RIGHT:
            case KeyEvent.VK_NUMPAD6:
                x = 1;
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_MINUS:
                _shapeDrawer.delete();
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_INSERT:
            case KeyEvent.VK_PLUS:
                _shapeDrawer.add(e.isShiftDown());
                break;
            default:
                return;

        }
        if (e.isShiftDown()) {
            x *= 5;
            y *= 5;
        }
        if (_selectionGroup != null) {
            for (Positionable comp : _selectionGroup) {
                moveItem(comp, x, y);
            }
        }
        repaint();
    }

    ///////////////// Handle mouse events ////////////////
    private long _mouseDownTime = 0;

    @Override
    public void mousePressed(MouseEvent event) {
        _mouseDownTime = System.currentTimeMillis();
        setToolTip(null); // ends tooltip if displayed
        log.debug("mousePressed at ({},{}) _dragging={}", event.getX(), event.getY(), _dragging);
        //  " _selectionGroup= "+(_selectionGroup==null?"null":_selectionGroup.size()));
        boolean circuitBuilder = _circuitBuilder.saveSelectionGroup(_selectionGroup);
        _anchorX = event.getX();
        _anchorY = event.getY();
        _lastX = _anchorX;
        _lastY = _anchorY;

        _currentSelection = getCurrentSelection(event);
        _circuitBuilder.doMousePressed(event, _currentSelection);

        if (!event.isPopupTrigger() && !event.isMetaDown() && !event.isAltDown() && !circuitBuilder) {
            _shapeDrawer.doMousePressed(event, _currentSelection);
            if (_currentSelection != null) {
                _currentSelection.doMousePressed(event);
                if (isEditable()) {
                    if (!event.isControlDown()
                            && (_selectionGroup != null && !_selectionGroup.contains(_currentSelection))) {
                        if (_pastePending) {
                            abortPasteItems();
                        }
                        deselectSelectionGroup();
                    }
                }
            } else {
                _highlightcomponent = null;
                if (_pastePending) {
                    abortPasteItems();
                }
                deselectSelectionGroup();
            }
        } else if (_currentSelection == null || (_selectionGroup != null && !_selectionGroup.contains(_currentSelection))) {
            deselectSelectionGroup();
        }
        _circuitBuilder.doMousePressed(event);
        _targetPanel.repaint(); // needed for ToolTip
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        _mouseDownTime = 0;
        setToolTip(null); // ends tooltip if displayed
        if (log.isDebugEnabled()) { // avoid string concatination if not debug
            log.debug("mouseReleased at ({},{}) dragging={}, pastePending={}, selectRect is{} null",
                    event.getX(), event.getY(), _dragging, _pastePending, (_selectRect == null ? "" : " not"));
        }
        Positionable selection = getCurrentSelection(event);

        if ((event.isPopupTrigger() || event.isMetaDown() || event.isAltDown()) /*&& !_dragging*/) {
            if (selection != null) {
                _highlightcomponent = null;
                showPopUp(selection, event);
            } else if (_selectRect != null) {
                makeSelectionGroup(event);
            }
        } else {
            if (selection != null) {
                selection.doMouseReleased(event);
            }
            // when dragging, don't change selection group
            if (_pastePending && _dragging) {
                pasteItems();
            }
            if (isEditable()) {
                _shapeDrawer.doMouseReleased(selection, event, this);

                if (!_circuitBuilder.doMouseReleased(selection, _dragging)) {
                    if (selection != null) {
                        if (!_dragging) {
                            modifySelectionGroup(selection, event);
                        }
                    }
                    if (_selectRect != null) {
                        makeSelectionGroup(event);
                    }
                    if (_currentSelection != null && (_selectionGroup == null || _selectionGroup.isEmpty())) {
                        if (_selectionGroup == null) {
                            _selectionGroup = new ArrayList<>();
                        }
                        _selectionGroup.add(_currentSelection);
                    }
                }
                _currentSelection = selection;
            } else {
                deselectSelectionGroup();
                _currentSelection = null;
                _highlightcomponent = null;
            }
        }
        _selectRect = null;

        // if not sending MouseClicked, do it here
        if (InstanceManager.getDefault(GuiLafPreferencesManager.class).isNonStandardMouseEvent()) {
            mouseClicked(event);
        }

        _lastX = event.getX();
        _lastY = event.getY();
        _dragging = false;
        _currentSelection = null;
        _targetPanel.repaint(); // needed for ToolTip
//        if (_debug) log.debug("mouseReleased at ("+event.getX()+","+event.getY()+
//        " _selectionGroup= "+(_selectionGroup==null?"null":_selectionGroup.size()));
    }

    private long _clickTime;

    @Override
    public void mouseClicked(MouseEvent event) {
        if (InstanceManager.getDefault(GuiLafPreferencesManager.class).isNonStandardMouseEvent()) {
            long time = System.currentTimeMillis();
            if (time - _clickTime < 20) {
                return;
            }
            _clickTime = time;
        }

        setToolTip(null); // ends tooltip if displayed
        log.debug("mouseClicked at ({},{})", event.getX(), event.getY());

        Positionable selection = getCurrentSelection(event);
        if (_shapeDrawer.doMouseClicked(event, this)) {
            return;
        }

        if (event.isPopupTrigger() || event.isMetaDown() || event.isAltDown()) {
            if (selection != null) {
                _highlightcomponent = null;
                showPopUp(selection, event);
            }
        } else if (selection != null) {
            if (_circuitBuilder.doMouseClicked(getSelectedItems(event), event)) {
                return;
            } else {
                selection.doMouseClicked(event);
            }
            if (selection instanceof IndicatorTrack) {
                WarrantTableAction.getDefault().mouseClickedOnBlock(((IndicatorTrack) selection).getOccBlock());
            }
        }
        if (!isEditable()) {
            deselectSelectionGroup();
            _currentSelection = null;
            _highlightcomponent = null;
        }
        _targetPanel.repaint(); // needed for ToolTip
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        //if (_debug) log.debug("mouseDragged at ("+event.getX()+","+event.getY()+")");
        setToolTip(null); // ends tooltip if displayed

        long time = System.currentTimeMillis();
        if (time - _mouseDownTime < 200) {
            return;     // don't drag until sure mouse down was not just a select click
        }
        _dragging = true;

        if (_circuitBuilder.doMouseDragged(_currentSelection, event)) {
            return;
        }

        if (!event.isPopupTrigger() && !event.isMetaDown() && !event.isAltDown() && !_shapeDrawer.doMouseDragged(event)
                && (isEditable() || _currentSelection instanceof LocoIcon)) {
            moveIt:
            if (_currentSelection != null && getFlag(OPTION_POSITION, _currentSelection.isPositionable())) {
                int deltaX = event.getX() - _lastX;
                int deltaY = event.getY() - _lastY;
                int minX = getItemX(_currentSelection, deltaX);
                int minY = getItemY(_currentSelection, deltaY);
                if (_selectionGroup != null && _selectionGroup.contains(_currentSelection)) {
                    for (Positionable comp : _selectionGroup) {
                        minX = Math.min(getItemX(comp, deltaX), minX);
                        minY = Math.min(getItemY(comp, deltaY), minY);
                    }
                }
                if (minX < 0 || minY < 0) {
                    // Don't allow move beyond the left or top borders
                    break moveIt;
                    /*
                     // or use this choice:
                     // Expand the panel to the left or top as needed by the move
                     // Probably not the preferred solution - use the above break
                     if (_selectionGroup!=null && _selectionGroup.contains(_currentSelection)) {
                     List <Positionable> allItems = getContents();
                     for (int i=0; i<allItems.size(); i++){
                     moveItem(allItems.get(i), -deltaX, -deltaY);
                     }
                     } else {
                     moveItem(_currentSelection, -deltaX, -deltaY);
                     }
                     */
                }
                if (_selectionGroup != null && _selectionGroup.contains(_currentSelection)
                        && !_circuitBuilder.dragPortal()) {
                    for (Positionable comp : _selectionGroup) {
                        moveItem(comp, deltaX, deltaY);
                    }
                    _highlightcomponent = null;
                } else {
                    moveItem(_currentSelection, deltaX, deltaY);
                }
            } else if ((isEditable() && _selectionGroup == null)) {
                drawSelectRect(event.getX(), event.getY());
            }
        }
        _highlightGroup.clear();
        _lastX = event.getX();
        _lastY = event.getY();
        _targetPanel.repaint(); // needed for ToolTip
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        //if (_debug) log.debug("mouseMoved at ("+event.getX()+","+event.getY()+")");
        if (_dragging || event.isPopupTrigger() || event.isMetaDown() || event.isAltDown()) {
            return;
        }
        if (!(event.isShiftDown() && event.isControlDown()) && !_shapeDrawer.doMouseMoved(event)) {
            Positionable selection = getCurrentSelection(event);
            if (selection != null && selection.getDisplayLevel() > BKG && selection.showToolTip()) {
                showToolTip(selection, event);
                //selection.highlightlabel(true);
            } else {
                setToolTip(null);
            }
        }
        _targetPanel.repaint();
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        _targetPanel.repaint();
    }

    @Override
    public void mouseExited(MouseEvent event) {
        setToolTip(null);
        _targetPanel.repaint();  // needed for ToolTip
    }

    ////////////////// implementation of Abstract Editor methods //////////////////
    /**
     * The target window has been requested to close, don't delete it at this
     * time. Deletion must be accomplished via the Delete this panel menu item.
     *
     * @param e the triggering event
     */
    @Override
    protected void targetWindowClosingEvent(java.awt.event.WindowEvent e) {
        targetWindowClosing();
    }

    @Override
    protected void paintTargetPanel(Graphics g) {
        // needed to create PositionablePolygon
        _shapeDrawer.paint(g);  // adds to rubber band line

        if (!_highlightGroup.isEmpty()) {
            g.setColor(((TargetPane) getTargetPanel()).getHighlightColor());
            for (Rectangle r : _highlightGroup) {
                g.drawRect(r.x, r.y, r.width, r.height);
            }
        }
    }

    /**
     * Set an object's location when it is created.
     */
    @Override
    public void setNextLocation(Positionable obj) {
        obj.setLocation(0, 0);
    }

    /**
     * Set up selections for a paste. Note a copy of _selectionGroup is made
     * that is NOT in the _contents. This disconnected ArrayList is added to the
     * _contents when (if) a paste is made. The disconnected _selectionGroup can
     * be dragged to a new location.
     */
    @Override
    protected void copyItem(Positionable p) {
        if (log.isDebugEnabled()) { // avoid string concatination if not debug
            log.debug("Enter copyItem: _selectionGroup size={}",
                    _selectionGroup != null ? _selectionGroup.size() : "(null)");
        }
        // If popup menu hit again, Paste selections and make another copy
        if (_pastePending) {
            pasteItems();
        }
        if (_selectionGroup != null && !_selectionGroup.contains(p)) {
            deselectSelectionGroup();
        }
        if (_selectionGroup == null) {
            _selectionGroup = new ArrayList<>();
            _selectionGroup.add(p);
        }
        ArrayList<Positionable> selectionGroup = new ArrayList<>();
        for (Positionable comp : _selectionGroup) {
            Positionable pos = comp.deepClone();
            selectionGroup.add(pos);
        }
        _selectionGroup = selectionGroup;  // group is now disconnected
        _pastePending = true;
        log.debug("Exit copyItem: _selectionGroup.size()={}", _selectionGroup.size());
    }

    void pasteItems() {
        if (_selectionGroup != null) {
            for (Positionable pos : _selectionGroup) {
                if (pos instanceof PositionableIcon) {
                    jmri.NamedBean bean = pos.getNamedBean();
                    if (bean != null) {
                        ((PositionableIcon) pos).displayState(bean.getState());
                    }
                }
                try {
                    pos.setId(null);
                    putItem(pos);
                } catch (Positionable.DuplicateIdException e) {
                    // This should never happen
                    log.error("Editor.putItem() with null id has thrown DuplicateIdException", e);
                }
                log.debug("Add {}", pos.getNameString());
            }
            if (_selectionGroup.get(0) instanceof LocoIcon) {
                LocoIcon p = (LocoIcon) _selectionGroup.get(0);
                CoordinateEdit f = new CoordinateEdit();
                f.init("Train Name", p, false);
                f.initText();
                f.setVisible(true);
                f.setLocationRelativeTo(p);
            }
        }
        _pastePending = false;
    }

    /**
     * Showing the popup of a member of _selectionGroup causes an image to be
     * placed in to the _targetPanel. If the objects are not put into _contents
     * (putItem(p)) the image will persist. Thus set these transitory object
     * invisible.
     */
    void abortPasteItems() {
        if (log.isDebugEnabled()) { // avoid string concatination if not debug
            log.debug("abortPasteItems: _selectionGroup{}",
                    _selectionGroup == null ? "=null" : (".size=" + _selectionGroup.size()));
        }
        if (_selectionGroup != null) {
            for (Positionable comp : _selectionGroup) {
                comp.setVisible(false);
                comp.remove();
            }
        }
        deselectSelectionGroup();
        _pastePending = false;
    }

    /**
     * Add an action to copy the Positionable item and the group to which is may
     * belong.
     *
     * @param p     the copyable item
     * @param popup the menu to add it to
     */
    public void setCopyMenu(Positionable p, JPopupMenu popup) {
        JMenuItem edit = new JMenuItem(Bundle.getMessage("MenuItemDuplicate"));
        edit.addActionListener(new ActionListener() {
            Positionable comp;

            @Override
            public void actionPerformed(ActionEvent e) {
                copyItem(comp);
            }

            ActionListener init(Positionable pos) {
                comp = pos;
                return this;
            }
        }.init(p));
        popup.add(edit);
    }

    @Override
    protected void setSelectionsScale(double s, Positionable p) {
        if (_circuitBuilder.saveSelectionGroup(_selectionGroup)) {
            p.setScale(s);
        } else {
            super.setSelectionsScale(s, p);
        }
    }

    @Override
    protected void setSelectionsRotation(int k, Positionable p) {
        if (_circuitBuilder.saveSelectionGroup(_selectionGroup)) {
            p.rotate(k);
        } else {
            super.setSelectionsRotation(k, p);
        }
    }

    /**
     * Create popup for a Positionable object. Popup items common to all
     * positionable objects are done before and after the items that pertain
     * only to specific Positionable types.
     */
    @Override
    protected void showPopUp(Positionable p, MouseEvent event) {
        if (!((JComponent) p).isVisible()) {
            return;     // component must be showing on the screen to determine its location
        }
        JPopupMenu popup = new JPopupMenu();

        PositionablePopupUtil util = p.getPopupUtility();
        if (p.isEditable()) {
            // items common to all
            if (p.doViemMenu()) {
                popup.add(p.getNameString());
                setPositionableMenu(p, popup);
                if (p.isPositionable()) {
                    setShowCoordinatesMenu(p, popup);
                    setShowAlignmentMenu(p, popup);
                }
                setDisplayLevelMenu(p, popup);
                setHiddenMenu(p, popup);
                setEditIdMenu(p, popup);
                popup.addSeparator();
                setCopyMenu(p, popup);
            }

            // items with defaults or using overrides
            boolean popupSet = false;
//            popupSet |= p.setRotateOrthogonalMenu(popup);
            popupSet |= p.setRotateMenu(popup);
            popupSet |= p.setScaleMenu(popup);
            if (popupSet) {
                popup.addSeparator();
                popupSet = false;
            }
            popupSet = p.setEditItemMenu(popup);
            if (popupSet) {
                popup.addSeparator();
                popupSet = false;
            }
            if (p instanceof PositionableLabel) {
                PositionableLabel pl = (PositionableLabel) p;
                if (pl.isText()) {
                    setColorMenu(popup, (JComponent) p, ColorDialog.BORDER);
                    setColorMenu(popup, (JComponent) p, ColorDialog.MARGIN);
                    setColorMenu(popup, (JComponent) p, ColorDialog.FONT);
                    if (!(pl instanceof ReporterIcon) && !(pl instanceof RpsPositionIcon)) {
                        popupSet |= pl.setEditTextItemMenu(popup);
                    }
                }
            } else if (p instanceof PositionableJPanel) {
                setColorMenu(popup, (JComponent) p, ColorDialog.BORDER);
                setColorMenu(popup, (JComponent) p, ColorDialog.MARGIN);
                setColorMenu(popup, (JComponent) p, ColorDialog.FONT);
                PositionableJPanel pj = (PositionableJPanel) p;
                popupSet |= pj.setEditTextItemMenu(popup);
            }
            if (p instanceof LinkingObject) {
                ((LinkingObject) p).setLinkMenu(popup);
            }
            if (popupSet) {
                popup.addSeparator();
                popupSet = false;
            }
            p.setDisableControlMenu(popup);
            if (util != null) {
                util.setAdditionalEditPopUpMenu(popup);
            }
            // for Positionables with unique settings
            p.showPopUp(popup);

            if (p.doViemMenu()) {
                setShowToolTipMenu(p, popup);
                setRemoveMenu(p, popup);
            }
        } else {
            if (p instanceof LocoIcon) {
                setCopyMenu(p, popup);
            }
            p.showPopUp(popup);
            if (util != null) {
                util.setAdditionalViewPopUpMenu(popup);
            }
        }
        popup.show((Component) p, p.getWidth() / 2 + (int) ((getPaintScale() - 1.0) * p.getX()),
                p.getHeight() / 2 + (int) ((getPaintScale() - 1.0) * p.getY()));

        _currentSelection = null;
    }

    public void setColorMenu(JPopupMenu popup, JComponent pos, int type) {
        String title;
        switch (type ) {
            case ColorDialog.BORDER:
                title = "SetBorderSizeColor";
                break;
            case ColorDialog.MARGIN:
                title = "SetMarginSizeColor";
                break;
            case ColorDialog.FONT:
                title = "SetFontSizeColor";
                break;
            case ColorDialog.TEXT:
                title = "SetTextSizeColor";
                break;
            default:
                title = "untitled";
                return;
        }
        JMenuItem edit = new JMenuItem(Bundle.getMessage(title));
        edit.addActionListener((ActionEvent event) -> new ColorDialog(this, pos, type, null));
        popup.add(edit);
    }

    /**
     * ******************* Circuitbuilder ***********************************
     */
    protected void disableMenus() {
        _drawMenu.setEnabled(false);
        _warrantMenu.setEnabled(false);
        _iconMenu.setEnabled(false);
        _zoomMenu.setEnabled(false);
        _optionMenu.setEnabled(false);
        _editMenu.setEnabled(false);
        _fileMenu.setEnabled(false);
        _disablePortalSelection = false;
    }

    public void resetEditor() {
        // enable menus
        _drawMenu.setEnabled(true);
        _warrantMenu.setEnabled(true);
        _iconMenu.setEnabled(true);
        _zoomMenu.setEnabled(true);
        _optionMenu.setEnabled(true);
        _editMenu.setEnabled(true);
        _fileMenu.setEnabled(true);
        // reset colors
        highlight(null);
        TargetPane targetPane = (TargetPane) getTargetPanel();
        targetPane.setDefaultColors();
        targetPane.revalidate();
        setSelectionGroup(null);
        _disablePortalSelection = true;
    }

    /**
     * Highlight an item.
     *
     * @param pos the item to hightlight
     */
    protected void highlight(Positionable pos) {
        if (pos == null) {
            _highlightGroup.clear();
            _highlightcomponent = null;
        } else {
            Rectangle rect = new Rectangle(pos.getX(), pos.getY(),
                    pos.maxWidth(), pos.maxHeight());
            _highlightcomponent = rect;
            if (!_dragging) {
                _highlightGroup.add(rect);
            }
        }
        repaint();
    }

    protected void setSelectionGroup(ArrayList<Positionable> group) {
        _highlightcomponent = null;
        _selectionGroup = group;
        repaint();
    }

    protected ArrayList<Positionable> getSelectionGroup() {
        return _selectionGroup;
    }

    /**
     * ************************** DnD *************************************
     */
    protected void makeDataFlavors() {
        try {
            _positionableDataFlavor = new DataFlavor(POSITIONABLE_FLAVOR);
            _namedIconDataFlavor = new DataFlavor(ImageIndexEditor.IconDataFlavorMime);
            _positionableListDataFlavor = new DataFlavor(List.class, "JComponentList");
        } catch (ClassNotFoundException cnfe) {
            log.error("Unable to find class supporting {}", ImageIndexEditor.IconDataFlavorMime, cnfe);
        }
        new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this);
    }

    DataFlavor _positionableDataFlavor;
    DataFlavor _positionableListDataFlavor;
    DataFlavor _namedIconDataFlavor;

    /**
     * ************************* DropTargetListener ***********************
     */
    @Override
    public void dragExit(DropTargetEvent evt) {
    }

    @Override
    public void dragEnter(DropTargetDragEvent evt) {
    }

    @Override
    public void dragOver(DropTargetDragEvent evt) {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent evt) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void drop(DropTargetDropEvent evt) {
        try {
            //Point pt = evt.getLocation(); coords relative to entire window
            Point pt = _targetPanel.getMousePosition(true);
            if (pt == null) {
                return;
            }
            Transferable tr = evt.getTransferable();
            if (log.isDebugEnabled()) { // avoid string building if not debug
                DataFlavor[] flavors = tr.getTransferDataFlavors();
                StringBuilder flavor = new StringBuilder();
                for (DataFlavor flavor1 : flavors) {
                    flavor.append(flavor1.getRepresentationClass().getName()).append(", ");
                }
                log.debug("Editor Drop: flavor classes={}", flavor);
            }
            if (tr.isDataFlavorSupported(_positionableDataFlavor)) {
                Positionable item = (Positionable) tr.getTransferData(_positionableDataFlavor);
                if (item == null) {
                    return;
                }
                item.setLocation((int) Math.round(pt.x/getPaintScale()), (int) Math.round(pt.y/getPaintScale()));
                // now set display level in the pane.
                item.setDisplayLevel(item.getDisplayLevel());
                item.setEditor(this);
                try {
                    putItem(item);
                } catch (Positionable.DuplicateIdException e) {
                    // This should never happen
                    log.error("Editor.putItem() with null id has thrown DuplicateIdException", e);
                }
                item.updateSize();
                _circuitBuilder.doMouseReleased(item, true);
                evt.dropComplete(true);
                return;
            } else if (tr.isDataFlavorSupported(_namedIconDataFlavor)) {
                NamedIcon newIcon = new NamedIcon((NamedIcon) tr.getTransferData(_namedIconDataFlavor));
                String url = newIcon.getURL();
                NamedIcon icon = NamedIcon.getIconByName(url);
                PositionableLabel ni = new PositionableLabel(icon, this);
                // infer a background icon from its size
                assert icon != null;
                if (icon.getIconHeight() > 500 || icon.getIconWidth() > 600) {
                    ni.setDisplayLevel(BKG);
                } else {
                    ni.setDisplayLevel(ICONS);
                }
                ni.setLocation((int) Math.round(pt.x/getPaintScale()), (int) Math.round(pt.y/getPaintScale()));
                ni.setEditor(this);
                try {
                    putItem(ni);
                } catch (Positionable.DuplicateIdException e) {
                    // This should never happen
                    log.error("Editor.putItem() with null id has thrown DuplicateIdException", e);
                }
                ni.updateSize();
                evt.dropComplete(true);
                return;
            } else if (tr.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) tr.getTransferData(DataFlavor.stringFlavor);
                PositionableLabel l = new PositionableLabel(text, this);
                l.setSize(l.getPreferredSize().width, l.getPreferredSize().height);
                l.setDisplayLevel(LABELS);
                l.setLocation((int) Math.round(pt.x/getPaintScale()), (int) Math.round(pt.y/getPaintScale()));
                l.setEditor(this);
                try {
                    putItem(l);
                } catch (Positionable.DuplicateIdException e) {
                    // This should never happen
                    log.error("Editor.putItem() with null id has thrown DuplicateIdException", e);
                }
                evt.dropComplete(true);
            } else if (tr.isDataFlavorSupported(_positionableListDataFlavor)) {
                List<Positionable> dragGroup
                        = (List<Positionable>) tr.getTransferData(_positionableListDataFlavor);
                for (Positionable pos : dragGroup) {
                    pos.setEditor(this);
                    pos.setLocation((int) Math.round(pt.x/getPaintScale()), (int) Math.round(pt.y/getPaintScale()));
                    try {
                        putItem(pos);
                    } catch (Positionable.DuplicateIdException ignore) {
                        try {
                            // Duplicate id so clear the id
                            pos.setId(null);
                            putItem(pos);
                        } catch (Positionable.DuplicateIdException e) {
                            // This should never happen
                            log.error("Editor.putItem() with null id has thrown DuplicateIdException", e);
                        }
                    }
                    pos.updateSize();
                    log.debug("DnD Add {}", pos.getNameString());
                }
            } else {
                log.warn("Editor DropTargetListener supported DataFlavors not available at drop from {}", tr.getClass().getName());
            }
        } catch (IOException ioe) {
            log.warn("Editor DropTarget caught IOException", ioe);
        } catch (UnsupportedFlavorException ufe) {
            log.warn("Editor DropTarget caught UnsupportedFlavorException", ufe);
        }
        log.debug("Editor DropTargetListener drop REJECTED!");
        evt.rejectDrop();
    }

    static protected class PositionableListDnD implements Transferable {
//        ControlPanelEditor _sourceEditor;

        List<Positionable> _sourceEditor;
        DataFlavor _dataFlavor;

        PositionableListDnD(List<Positionable> source) {
            _sourceEditor = source;
            _dataFlavor = new DataFlavor(List.class, "JComponentList");
        }

        @Override
        @Nonnull
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            log.debug("PositionableListDnD.getTransferData:");
            if (flavor.equals(_dataFlavor)) {
                return _sourceEditor;
            }
            throw new UnsupportedFlavorException(flavor);
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{_dataFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(_dataFlavor);
        }
    }

    private final static Logger log = LoggerFactory.getLogger(ControlPanelEditor.class);
}
