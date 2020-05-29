package com.lttlgames.editor;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import sun.swing.DefaultLookup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import com.lttlgames.editor.GuiInputController.PressedType;
import com.lttlgames.editor.LttlTransform.LttlTransformChangeType;
import com.lttlgames.helpers.EaseType;
import com.lttlgames.helpers.LttlCallback;
import com.lttlgames.helpers.LttlGeometry;
import com.lttlgames.helpers.LttlGeometryUtil;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlObjectHelper;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GuiSelectionController implements TreeSelectionListener,
		TreeExpansionListener
{
	private static JPanel panel;
	private JTree tree;
	private DefaultTreeModel model;
	private JScrollPane scrollPane;
	// just something to fix a bug where valueChange is called twice sometimes
	private IntArray lastSelectedTransforms = new IntArray();
	// on a single transform selection change
	JPopupMenu editorPopupMenu = null;

	// used for maintaining selection when changing play<->editor mode
	private static int prevSelectedSceneId = -1;
	private static int prevSelectedTransformId = -1;

	/**
	 * Only gets populated/updated when popup menu is created. This includes other nodes if right clicked on a node that
	 * was part of a selection. Otherwise this is the same as popupTargetNode.
	 */
	private ArrayList<LttlNode> popupMenuNodeObjects = new ArrayList<GuiSelectionController.LttlNode>();
	/**
	 * This specifies if the node that was rightclicked is part of a selection of nodes, since you can have a selection
	 * but not right click i
	 */
	private boolean popupTargetInSelection;

	/**
	 * This is the actual node in the tree that was targetd/right clicked, regardless of selection
	 */
	LttlNode popupTargetNode;

	/**
	 * This is the bounding rectangle for all the mutliSelected transforms.
	 */
	private Rectangle multiSelectRectangle = new Rectangle();

	/**
	 * The box drawn when selecting transforms. If null, means not in select box state.
	 */
	Rectangle selectBoxRectangle = null;
	private Rectangle zoomBoxRectangle = null;

	// Handles
	private HandleRect handleRot;
	private HandleRect handleScale;
	private HandleRect handlePos;
	private HandleRect handleMultiPos;
	private HandleRect handleMultiScale;
	private HandleCircle handleCameraRot;
	private HandleCircle handleCameraZoom;
	private HandleCircle handleCameraPos;

	private LttlTransform highlightedTransform;

	private Rectangle tempRect = new Rectangle();

	private ArrayList<Color> hiearchyTreeColors = new ArrayList<Color>();
	{
		hiearchyTreeColors.add(new Color(Color.MAGENTA));
		hiearchyTreeColors.add(new Color(Color.CYAN));
		hiearchyTreeColors.add(new Color(Color.GREEN));
		hiearchyTreeColors.add(new Color(Color.ORANGE));
		hiearchyTreeColors.add(new Color(Color.PINK));
		hiearchyTreeColors.add(new Color(Color.RED));
		hiearchyTreeColors.add(new Color(Color.BLUE));
	}

	// temp
	private Color tmpColor = new Color();
	private Vector2 tmpV2 = new Vector2();

	// private Polygon tmpPolygon = new Polygon();

	GuiSelectionController()
	{
		// only create panel if first time
		if (panel == null)
		{
			// Borderlayout center allows it to auto resize to it's parent
			// add scroll pane to this JPanel
			panel = new JPanel();
			panel.setLayout(new GridBagLayout());
			panel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(
					3, 0, 6, 6), BorderFactory
					.createTitledBorder("Transform Trees")));
		}
		// if there is any previous stuff, clear it
		panel.removeAll();
		init();
	}

	private void init()
	{
		// create tree
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(new LttlNode());
		tree = new JTree(root);

		model = (DefaultTreeModel) tree.getModel();
		tree.addTreeExpansionListener(this);
		tree.setOpaque(false);
		tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setCellRenderer(new LttlTreeCellRenderer());

		// change font size
		GuiHelper.SetFontSize(tree, 10);
		// disable icons
		DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree
				.getCellRenderer();
		renderer.setLeafIcon(null);
		renderer.setClosedIcon(null);
		renderer.setOpenIcon(null);
		// change indent size
		BasicTreeUI basicTreeUI = (BasicTreeUI) tree.getUI();
		basicTreeUI.setRightChildIndent(1);

		addPopupMenus();

		// create scroll pane, add tree, add to panel
		scrollPane = new JScrollPane(tree);
		scrollPane.setBorder(null);
		panel.add(scrollPane, new GridBagConstraints(0, 0, 1, 1, 1, 1,
				GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(
						-5, 0, 0, 0), 0, 0));

	}

	private void addPopupMenus()
	{
		final JPopupMenu transformPopup = new JPopupMenu();
		final JPopupMenu scenePopup = new JPopupMenu();
		final JPopupMenu treePopup = new JPopupMenu();

		// ** TRANSFORM(S) POPUP MENU **
		// MOVE COMPONENT
		final JMenuItem moveMenuItem = new JMenuItem("Move");
		moveMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().moveTransformIds.clear();
				if (popupTargetInSelection)
				{
					for (LttlNode ln : popupMenuNodeObjects)
					{
						Lttl.editor.getGui().moveTransformIds.add(ln.id);
					}
				}
				else
				{
					Lttl.editor.getGui().moveTransformIds
							.add(popupTargetNode.id);
				}
			}
		});
		transformPopup.add(moveMenuItem);

		// MOVE TRANSFORM
		final JMenuItem placeMenuItem = new JMenuItem("Place");
		placeMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// iterate through all the moved components, cast to Transforms (already checked) and add as children
				// to the popupTargetNode, which has already been checkd to be a transform
				LttlTransform target = (LttlTransform) Lttl.scenes
						.findComponentByIdAllScenes(popupTargetNode.id);
				ArrayList<LttlTransform> movers = Lttl.editor.getGui()
						.getMoveTransforms();
				if (movers.contains(target))
				{
					JOptionPane.showMessageDialog(null,
							"Can't move transform '" + target.getName()
									+ "' to self. No changes made.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				for (LttlTransform mover : movers)
				{
					if (!mover.isInSameScene(target))
					{
						JOptionPane
								.showMessageDialog(
										null,
										"Can't move transform '"
												+ mover.getName()
												+ "' to '"
												+ target.getName()
												+ "' becase they're in different scenes.  Copy/Move to scene first. No changes made.",
										"Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				for (LttlTransform mover : movers)
				{
					mover.setParent(target, true);
				}
				if (movers.size() == 1)
				{
					setSelection(movers.get(0));
				}

				// clear the moveTransformIds
				Lttl.editor.getGui().moveTransformIds.clear();
			}
		});
		transformPopup.add(placeMenuItem);

		// CAPTURE COMPONENT
		final JMenuItem captureMenuItem = new JMenuItem("Capture");
		captureMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().setCaptureComponent(popupTargetNode.id);
			}
		});
		transformPopup.add(captureMenuItem);

		// ADD TRANSFORM
		final JMenuItem addTransformMenuItem = new JMenuItem("Add Transform");
		addTransformMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().addTransformDialog(
						getPopupMenuFocusedTransforms(), null);
			}
		});
		transformPopup.add(addTransformMenuItem);

		// ADD COMPONENT
		final JMenuItem addComponentMenuItem = new JMenuItem("Add Component");
		addComponentMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().getMenuBarController()
						.addComponentDialog(getPopupMenuFocusedTransforms());
			}
		});
		transformPopup.add(addComponentMenuItem);

		// LOOK AT
		final JMenuItem lookAtMenuItem = new JMenuItem("Look At");
		lookAtMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getCamera().lookAt(getPopupMenuFocusedTransforms(),
						EaseType.QuadOut, 1f);
			}
		});
		transformPopup.add(lookAtMenuItem);

		// Go To
		final JMenuItem goToMenuItem = new JMenuItem("Go To");
		goToMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getCamera().goTo(getPopupMenuFocusedTransforms(),
						EaseType.QuadInOut, 1);
			}
		});
		transformPopup.add(goToMenuItem);

		// MOVE TO VIEW
		final JMenuItem moveToViewMenuItem = new JMenuItem("Move To View");
		moveToViewMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				for (LttlTransform transform : getPopupMenuFocusedTransforms())
				{
					transform
							.tweenPosTo(
									transform.worldToLocalPosition(
											Lttl.editor.getCamera().position,
											true), 1)
							.setEase(EaseType.QuadInOut).start();
				}
			}
		});
		transformPopup.add(moveToViewMenuItem);

		// ENABLE
		final JMenuItem enabledMenuItem = new JMenuItem("Enable");
		enabledMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				for (LttlTransform transform : getPopupMenuFocusedTransforms())
				{
					transform.enable();
				}
			}
		});
		transformPopup.add(enabledMenuItem);

		// DISABLE
		final JMenuItem disableMenuItem = new JMenuItem("Disable");
		disableMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				for (LttlTransform transform : getPopupMenuFocusedTransforms())
				{
					transform.disable();
				}
			}
		});
		transformPopup.add(disableMenuItem);

		// COPY TRANSFORM
		final JMenuItem copyTransformsMenuItem = new JMenuItem("Copy");
		copyTransformsMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().setCopyTransforms(
						getPopupMenuFocusedTransforms());
			}
		});
		transformPopup.add(copyTransformsMenuItem);

		// PASTE TRANSFORM(S)
		final JMenuItem pasteTransformsMenuItem = new JMenuItem(
				"Paste Transform(s)");
		pasteTransformsMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// check if can process paste based on same scenes
				for (LttlTransform transform : getPopupMenuFocusedTransforms())
				{
					for (LttlTransform lt : Lttl.editor.getGui()
							.getCopyTransforms())
					{
						if (transform.getScene() != lt.getScene())
						{
							JOptionPane.showMessageDialog(
									null,
									"Can't copy "
											+ lt.getName()
											+ " to "
											+ transform.getName()
											+ " in another scene.  Copy the transform(s) to the other scene first.  No changes made.",
									"Copy Failed - Different Scenes",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
					}
				}
				// process paste
				LttlObjectHelper.copyTransformsToTransforms(
						getPopupMenuFocusedTransforms(), Lttl.editor.getGui()
								.getCopyTransforms(), true, true);
			}
		});
		transformPopup.add(pasteTransformsMenuItem);

		// PASTE COMPONENT
		final JMenuItem pasteComponentMenuItem = new JMenuItem(
				"Paste Component");
		pasteComponentMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				pasteComponent(getPopupMenuFocusedTransforms());
			}
		});
		transformPopup.add(pasteComponentMenuItem);

		// ORDER
		final JMenu orderMenu = new JMenu("Order");
		orderMenu
				.setToolTipText("This is the order the transform's update methods will be called.");
		final LttlCallback orderClosure = new LttlCallback()
		{
			@Override
			public void callback(int direction, Object... objects)
			{
				ArrayList<LttlTransform> focusedTransforms = getPopupMenuFocusedTransforms();
				if (focusedTransforms.size() != 1) return;

				LttlTransform focused = focusedTransforms.get(0);

				DefaultMutableTreeNode sceneNode = getSceneNode(focused
						.getScene().getId());
				DefaultMutableTreeNode parentNode;

				if (focused.getParent() != null)
				{
					// has parent
					int index = focused.getParent().children.indexOf(focused);
					focused.getParent().moveChildOrder(index,
							direction == 0 ? 0 : index + direction);
					parentNode = getTransformNode(focused.getParent(),
							sceneNode);
				}
				else
				{
					// is top level transform
					LttlScene parentScene = focused.getScene();
					int index = parentScene.getRef().transformHiearchy
							.indexOf(focused);
					parentScene.moveTopLevelTransformsOrder(index,
							direction == 0 ? 0 : index + direction);
					parentNode = sceneNode;
				}
				reloadModelMaintainExpand(parentNode);
			}
		};
		final JMenuItem moveToTopMenuItem = new JMenuItem("Move To Top");
		moveToTopMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				orderClosure.callback(0);
			}
		});
		orderMenu.add(moveToTopMenuItem);
		final JMenuItem moveUpMenuItem = new JMenuItem("Move Up");
		moveUpMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				orderClosure.callback(-1);
			}
		});
		orderMenu.add(moveUpMenuItem);
		final JMenuItem moveDownMenuItem = new JMenuItem("Move Down");
		moveDownMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				orderClosure.callback(1);
			}
		});
		orderMenu.add(moveDownMenuItem);
		transformPopup.add(orderMenu);

		// DUPLICATE TRANSFORM
		final JMenuItem duplicateMenuItem = new JMenuItem("Duplicate");
		duplicateMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				for (LttlTransform lt : getPopupMenuFocusedTransforms())
				{
					lt.duplicate(true);
				}
			}
		});
		transformPopup.add(duplicateMenuItem);

		// DELETE TRANSFORM
		final JMenuItem deleteTransformMenuItem = new JMenuItem("Delete");
		deleteTransformMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().deleteTransformsDialog(
						getPopupMenuFocusedTransforms());
			}
		});
		transformPopup.add(deleteTransformMenuItem);

		// ** SCENE POPUP MENU **
		// ADD ID FIELD
		final JMenuItem idSceneMenuItem = new JMenuItem();
		scenePopup.add(idSceneMenuItem);

		// MOVE TRANSFORM
		final JMenuItem placeSceneMenuItem = new JMenuItem("Place");
		placeSceneMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				LttlScene scene = getPopupMenuFocusedScene();
				ArrayList<LttlTransform> moveTransforms = Lttl.editor.getGui()
						.getMoveTransforms();
				Lttl.editor.getGui().moveTransformIds.clear();

				for (LttlTransform transform : new ArrayList<LttlTransform>(
						moveTransforms))
				{
					if (transform.getScene() == scene)
					{
						// it's already in this scene, just remove any parents
						transform.setParent(null, true);
						moveTransforms.add(transform);
					}
					else
					{
						// need to move/cut it to this scene, save the new transform to moveTransforms list
						moveTransforms.add(transform.getScene().moveTransform(
								transform, scene));
					}
				}

				// if one move transform then set it as selection
				if (moveTransforms.size() == 1)
				{
					setSelection(moveTransforms.get(0));
				}

				// clear the moveComponentIds
				Lttl.editor.getGui().moveTransformIds.clear();
			}
		});
		scenePopup.add(placeSceneMenuItem);

		// ADD TRANSFORM
		final JMenuItem addTransformSceneMenuItem = new JMenuItem(
				"Add Transform");
		addTransformSceneMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().addTransformDialog(null,
						getPopupMenuFocusedScene());
			}
		});
		scenePopup.add(addTransformSceneMenuItem);

		// PASTE TRANSFORM(S)
		final JMenuItem pasteTransformsSceneMenuItem = new JMenuItem(
				"Paste Transform(s)");
		pasteTransformsSceneMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				LttlScene scene = getPopupMenuFocusedScene();

				for (LttlTransform lt : Lttl.editor.getGui()
						.getCopyTransforms())
				{
					scene.addTransformCopy(lt, true, true);
				}
			}
		});
		scenePopup.add(pasteTransformsSceneMenuItem);

		// SET NAME
		final JMenuItem setNameSceneMenuItem = new JMenuItem("Set Name");
		setNameSceneMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().setSceneNameDialog(
						getPopupMenuFocusedScene());
			}
		});
		scenePopup.add(setNameSceneMenuItem);

		// DUPLICATE
		final JMenuItem duplicateSceneMenuItem = new JMenuItem("Duplicate");
		duplicateSceneMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String name = getPopupMenuFocusedScene().getName();
				while (Lttl.scenes.sceneNameExists(name))
				{
					name += "_copy";
				}
				getPopupMenuFocusedScene().copyScene(name);
			}
		});
		scenePopup.add(duplicateSceneMenuItem);

		// UNLOAD SCENE
		final JMenuItem unloadSceneMenuItem = new JMenuItem("Unload");
		unloadSceneMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				LttlScene scene = getPopupMenuFocusedScene();

				// check if there are any dependencies
				ArrayList<LttlComponent> dependencies = scene.getDependencies();
				if (dependencies.size() > 0)
				{
					String depString = "";
					for (int i = 0; i < dependencies.size(); i++)
					{
						depString += dependencies.get(i).toString()
								+ ((i == dependencies.size() - 1) ? "" : ", ");
					}
					if (GuiHelper
							.showOptionModal(
									"Component Reference Dependencies",
									"The following components are being referenced by other components in other scenes (may not be gui): "
											+ depString
											+ ".  Would you like to continue unloading this scene and hard remove all references?") > 0) { return; }

				}
				getPopupMenuFocusedScene().unload(true);
			}
		});
		scenePopup.add(unloadSceneMenuItem);

		// DELETE SCENE
		final JMenuItem deleteSceneMenuItem = new JMenuItem("Delete");
		deleteSceneMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				LttlScene scene = getPopupMenuFocusedScene();

				// confirm?
				if (GuiHelper.showOptionModal(
						"Delete Scene",
						"Are you sure you want to delete scene: "
								+ scene.getName() + " [" + scene.getId() + "]") > 0) { return; }

				// check if there are any dependencies
				ArrayList<LttlComponent> dependencies = scene.getDependencies();
				if (dependencies.size() > 0)
				{
					String depString = "";
					for (int i = 0; i < dependencies.size(); i++)
					{
						depString += dependencies.get(i).toString()
								+ ((i == dependencies.size() - 1) ? "" : ", ");
					}
					if (GuiHelper
							.showOptionModal(
									"Component Reference Dependencies",
									"The following components are being referenced by other components in other scenes (may not be gui): "
											+ depString
											+ ".  Would you like to continue deleting this scene and hard remove all references?") > 0) { return; }

				}

				Lttl.scenes.deleteScene(scene.getId());
			}
		});
		scenePopup.add(deleteSceneMenuItem);

		// ** TREE POPUP MENU **
		// Load Scene
		final JMenuItem loadSceneMenuItem = new JMenuItem("Load Scene");
		loadSceneMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().loadSceneDialog();
			}
		});
		treePopup.add(loadSceneMenuItem);

		// Create Scene
		final JMenuItem createSceneMenuItem = new JMenuItem("Create Scene");
		createSceneMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().createSceneDialog();
			}
		});
		treePopup.add(createSceneMenuItem);

		// Delete Scene
		final JMenuItem deleteSceneDialogMenuItem = new JMenuItem(
				"Delete Scene");
		deleteSceneDialogMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().deleteSceneDialog();
			}
		});
		treePopup.add(deleteSceneDialogMenuItem);

		// ** POPUP MOUSE LISTENER **
		MouseListener treeMouseListener = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				action(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				action(e);
			}

			private void action(MouseEvent e)
			{
				if (!e.isPopupTrigger()) return;

				popupMenuNodeObjects.clear();
				popupTargetNode = null;

				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				// check if clicked on a tree node, if not then show tree popup
				if (selRow == -1)
				{
					// check if any scenes to be loaded
					loadSceneMenuItem.setEnabled(!Lttl.scenes
							.areAllScenesLoaded());

					// only enable create scenes if not playing
					createSceneMenuItem.setEnabled(!Lttl.game.isPlaying());

					// only delete scenes if not playing and more than one exists
					deleteSceneDialogMenuItem.setEnabled(!Lttl.game.isPlaying()
							&& Lttl.scenes.getSceneCount() > 0);

					// show tree popup menu
					treePopup.show(e.getComponent(), e.getX(), e.getY());
					return;
				}

				// save the node that was right clicked
				popupTargetNode = getNodeObject(selPath);

				popupTargetInSelection = false;

				// that means multiple transforms
				if (tree.getSelectionCount() > 1
						&& tree.getSelectionPaths() != null)
				{
					// check if the node clicked is one of the ones in the selection, so can apply popup menu to
					// entire selection
					for (TreePath tp : tree.getSelectionPaths())
					{
						// HACK
						// i have no idea why this can't be (selPath == tp)
						if (selPath.hashCode() == tp.hashCode())
						{
							popupTargetInSelection = true;
							break;
						}

					}
				}

				if (popupTargetInSelection)
				{
					// they right clicked on selection, so include all the transforms selected
					for (TreePath tp : tree.getSelectionPaths())
					{
						LttlNode ln = getNodeObject(tp);
						popupMenuNodeObjects.add(ln);
					}
				}
				else
				{
					// just add the node that was right clicked
					popupMenuNodeObjects.add(popupTargetNode);
				}

				// prepare and show popup menu
				// SCENE
				if (popupTargetNode.isScene)
				{
					// disable/enable paste menu items based on if a paste object exists
					pasteTransformsSceneMenuItem.setEnabled(Lttl.editor
							.getGui().getCopyTransforms().size() > 0);
					unloadSceneMenuItem
							.setEnabled(popupTargetNode.id != Lttl.scenes.WORLD_ID);
					setNameSceneMenuItem
							.setEnabled(popupTargetNode.id != Lttl.scenes.WORLD_ID);
					idSceneMenuItem.setText("ID: " + popupTargetNode.id);
					idSceneMenuItem.setEnabled(false);

					placeSceneMenuItem.setEnabled(false);
					if (Lttl.editor.getGui().moveTransformIds.size > 0)
					{
						placeSceneMenuItem.setEnabled(true);
						for (LttlComponent comp : Lttl.editor.getGui()
								.getMoveTransforms())
						{
							if (!(comp instanceof LttlTransform))
							{
								placeSceneMenuItem.setEnabled(false);
								break;
							}
						}
					}
					deleteSceneMenuItem.setEnabled(!Lttl.game.isPlaying()
							&& popupTargetNode.id != Lttl.scenes.WORLD_ID);

					duplicateSceneMenuItem.setEnabled(!Lttl.game.isPlaying());

					// show scene popup menu
					scenePopup.show(e.getComponent(), e.getX(), e.getY());
				}
				else
				// TRANSFORM(S)
				{
					ArrayList<LttlTransform> focusedTransforms = getPopupMenuFocusedTransforms();

					// disable/enable paste menu items based on if a paste object exists
					pasteComponentMenuItem.setEnabled(Lttl.editor.getGui()
							.getCopyComponent() != null);
					pasteTransformsMenuItem.setEnabled(Lttl.editor.getGui()
							.getCopyTransforms().size() > 0);

					// enabled the move here menu item only if there is at least one move component id and all of the
					// moved components are transforms
					placeMenuItem.setEnabled(false);
					if (Lttl.editor.getGui().moveTransformIds.size > 0)
					{
						placeMenuItem.setEnabled(true);
						for (LttlComponent comp : Lttl.editor.getGui()
								.getMoveTransforms())
						{
							if (!(comp instanceof LttlTransform))
							{
								placeMenuItem.setEnabled(false);
								break;
							}
						}
					}

					// check if any are disabled or enabled
					boolean atleastOneDisabled = false;
					boolean atleastOneEnabled = false;
					for (LttlTransform transform : focusedTransforms)
					{
						if (transform == null) continue;

						if (transform.isEnabled)
						{
							atleastOneEnabled = true;
						}
						else
						{
							atleastOneDisabled = true;
						}
					}

					// only enable relevant menu items
					enabledMenuItem.setEnabled(atleastOneDisabled);
					disableMenuItem.setEnabled(atleastOneEnabled);

					// check if can modify order
					int canModifyOrder = canModifyOrder(focusedTransforms);
					moveDownMenuItem.setEnabled(canModifyOrder >= 2);
					moveUpMenuItem.setEnabled(canModifyOrder == 1
							|| canModifyOrder == 3);
					moveToTopMenuItem.setEnabled(moveUpMenuItem.isEnabled());
					orderMenu.setEnabled(moveDownMenuItem.isEnabled()
							|| moveUpMenuItem.isEnabled());

					// show transform(s) popup menu
					transformPopup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		};
		tree.addMouseListener(treeMouseListener);

		MouseAdapter doubleClickAdapter = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				if (selRow != -1)
				{
					if (e.getClickCount() == 1)
					{
						// SINGLE CLICK
					}
					else if (e.getClickCount() == 2)
					{
						// DOUBLE CLICK
						LttlNode node = getNodeObject(selPath);
						if (!node.isScene)
						{
							Lttl.editor
									.getCamera()
									.lookAt((LttlTransform) Lttl.scenes
											.findComponentByIdAllScenes(node.id),
											EaseType.QuadOut, 1f);
						}
					}
				}
			}
		};
		tree.addMouseListener(doubleClickAdapter);
	}

	/**
	 * @return [0-none, 1-up,2-down,3-both]
	 */
	private int canModifyOrder(
			ArrayList<LttlTransform> popUpMenuFocusedTransforms)
	{
		if (popUpMenuFocusedTransforms.size() != 1) return 0;

		LttlTransform focused = popUpMenuFocusedTransforms.get(0);

		int index;
		int size;
		if (focused.getParent() != null)
		{
			// has parent
			index = focused.getParent().children.indexOf(focused);
			size = focused.getParent().children.size();
		}
		else
		{
			// is top level transform
			LttlScene parentScene = focused.getScene();
			index = parentScene.getRef().transformHiearchy.indexOf(focused);
			size = parentScene.getRef().transformHiearchy.size();
		}

		boolean canUp;
		boolean canDown;

		canUp = index > 0;
		canDown = index < size - 1;

		if (canUp && canDown)
		{
			return 3;
		}
		else if (canUp) return 1;
		else if (canDown) return 2;
		else return 0;
	}

	private ArrayList<LttlTransform> getPopupMenuFocusedTransforms()
	{
		ArrayList<LttlTransform> list = new ArrayList<LttlTransform>();

		for (LttlNode ln : popupMenuNodeObjects)
		{
			// Lttl.dump(ln.id);
			if (ln.isScene) continue;
			LttlTransform transform = (LttlTransform) Lttl.scenes
					.findComponentByIdAllScenes(ln.id);
			if (transform == null) continue;

			list.add(transform);
		}

		return list;
	}

	private LttlScene getPopupMenuFocusedScene()
	{
		if (popupTargetNode.isScene) { return Lttl.scenes
				.get(popupTargetNode.id); }
		return null;
	}

	// callback for tree selection change, included when removing a transform or scene
	@Override
	public void valueChanged(TreeSelectionEvent e)
	{
		if (isSelectionLocked()) return;

		// multi select
		if (tree.getSelectionPaths() != null && tree.getSelectionCount() > 1)
		{
			Array<TreePath> array = new Array<TreePath>(
					tree.getSelectionPaths());

			// check if selected a scene with transforms
			for (Iterator<TreePath> it = array.iterator(); it.hasNext();)
			{
				TreePath tp = it.next();

				LttlNode nodeObject = getNodeObject(tp);
				if (nodeObject.isScene)
				{
					// prevent scenes from being in a multi select, just confusing
					tree.getSelectionModel().clearSelection();
					tree.setSelectionPath(tp);

					// Properties Controller Callback
					Lttl.editor.getGui().getPropertiesController().draw(true);
				}
				else
				{
					// else it must be only transforms selected

					// remove descendants
					ArrayList<LttlTransform> selectedTransforms = getSelectedTransforms();
					if (LttlObjectHelper.RemoveDescendants(selectedTransforms) > 0)
					{
						// reset selection if any were removed
						setSelection(selectedTransforms);
						return;
					}

					// Properties Controller Callback
					Lttl.editor.getGui().getPropertiesController().draw(true);

					// add to transform history if change
					onTransformSelectionChange(selectedTransforms);

				}
			}

		}
		// single select
		else if (tree.getSelectionPaths() != null
				&& tree.getSelectionCount() == 1)
		{
			LttlNode nodeObject = getNodeObject(tree.getSelectionPath());
			if (nodeObject.isScene)
			{
				if (Lttl.scenes.isSceneLoaded(nodeObject.id))
				{
					// Properties Controller Callback
					Lttl.editor.getGui().getPropertiesController().draw(true);
				}
				else
				{
					tree.clearSelection();
				}
			}
			else
			{
				// Properties Controller Callback
				Lttl.editor.getGui().getPropertiesController().draw(true);

				// add to transform history
				onTransformSelectionChange(getSelectedTransforms());
			}
		}
		else
		// no selection
		{
			Lttl.editor.getGui().getPropertiesController().draw(true);
		}

		// update handles
		// this slight delay ensures there has been one frame rendered, and meshes have been created, so selection
		// handles will be accurate
		if (Lttl.game.getFrameCount() > 1)
		{
			updateHandles();
		}

		// save the current selection to the editor settings
		// but don't worry about selection changes when game is still initially loading
		if (Lttl.game.getState() != GameState.SETTINGUP)
		{
			Lttl.game.getWorldCore().editorSettings.initialSelectedTransforms = getSelectedTransformIds();
		}
	}

	/**
	 * How many transforms are selected.
	 * 
	 * @return
	 */
	public int getSelectedTransformCount()
	{
		int count = 0;
		if (tree.getSelectionPaths() != null)
		{
			for (TreePath tp : tree.getSelectionPaths())
			{
				LttlNode nodeObject = getNodeObject(tp);
				if (!nodeObject.isScene)
				{
					count++;
				}
			}
		}

		return count;
	}

	/**
	 * Checks if transform is the only transform selected.
	 * 
	 * @param transform
	 * @return
	 */
	public boolean isSingleSelected(LttlTransform transform)
	{
		if (getSelectedTransformCount() == 1
				&& getSelectedTransforms().contains(transform)) { return true; }
		return false;
	}

	public boolean isSceneSelected()
	{
		return getSelectedScene() != null;
	}

	/**
	 * Selecting multiple transforms in editor view
	 * 
	 * @param transform
	 */
	public void addSelection(LttlTransform transform)
	{
		if (Lttl.editor.getGui().disableGuiRefresh) { return; }

		Array<TreePath> array = new Array<TreePath>(tree.getSelectionModel()
				.getSelectionPaths());
		TreePath tp = getTreePath(getTransformNode(transform));
		if (!array.contains(tp, true))
		{
			array.add(tp);
		}

		tree.getSelectionModel().setSelectionPaths(array.toArray());
	}

	/**
	 * If already selected, deselects, if not, then adds it to selection
	 * 
	 * @param transform
	 */
	public void toggleSelection(LttlTransform transform)
	{
		Array<TreePath> array = new Array<TreePath>(tree.getSelectionModel()
				.getSelectionPaths());
		TreePath tp = getTreePath(getTransformNode(transform));
		if (array.contains(tp, false))
		{
			array.removeValue(tp, false);
		}
		else
		{
			array.add(tp);
		}

		tree.getSelectionModel().setSelectionPaths(array.toArray());
	}

	/**
	 * Checks Control or Shift modifier to see if should set selection or toggle.
	 * 
	 * @param transform
	 */
	public void selectionWithModifiers(LttlTransform transform)
	{
		if (Lttl.editor.getInput().isControlEV()
				|| Lttl.editor.getInput().isShiftEV())
		{
			Lttl.editor.getGui().getSelectionController()
					.toggleSelection(transform);
		}
		else
		{
			Lttl.editor.getGui().getSelectionController()
					.setSelection(transform);
		}
	}

	/**
	 * remove selected transform from selection
	 * 
	 * @param transform
	 */
	public void removeSelectedTransform(LttlTransform transform)
	{
		if (Lttl.editor.getGui().disableGuiRefresh) { return; }

		Array<TreePath> array = new Array<TreePath>(tree.getSelectionModel()
				.getSelectionPaths());
		array.removeValue((getTreePath(getTransformNode(transform))), true);

		tree.getSelectionModel().setSelectionPaths(array.toArray());
	}

	/**
	 * Select a LttlScene in editor view
	 * 
	 * @param scene
	 */
	public void setSelectedScene(LttlScene scene)
	{
		if (isSelectionLocked()) return;
		tree.getSelectionModel().setSelectionPath(
				getTreePath(getSceneNode(scene.getId())));
	}

	/**
	 * Select a transform in editor view
	 * 
	 * @param transform
	 */
	public void setSelection(LttlTransform transform)
	{
		if (isSelectionLocked()) return;
		if (transform == null)
		{
			clearSelection();
			return;
		}
		tree.getSelectionModel().setSelectionPath(
				getTreePath(getTransformNode(transform)));
	}

	/**
	 * Sets the selection. If the selection is the same, it ignores the request.
	 * 
	 * @param transforms
	 */
	public void setSelection(ArrayList<LttlTransform> transforms)
	{
		if (isSelectionLocked()) return;
		ArrayList<LttlTransform> currentSelection = Lttl.editor.getGui()
				.getSelectionController().getSelectedTransforms();
		if (transforms.size() != currentSelection.size()
				|| !currentSelection.containsAll(transforms))
		{
			TreePath[] selection = new TreePath[transforms.size()];
			for (int i = 0; i < selection.length; i++)
			{
				selection[i] = getTreePath(getTransformNode(transforms.get(i)));
			}
			tree.getSelectionModel().setSelectionPaths(selection);
		}
	}

	/**
	 * Returns all the transforms that are selected in the tree.
	 * 
	 * @return
	 */
	public ArrayList<LttlTransform> getSelectedTransforms()
	{
		ArrayList<LttlTransform> list = new ArrayList<LttlTransform>();

		if (tree.getSelectionPaths() != null)
		{
			for (TreePath tp : tree.getSelectionPaths())
			{
				LttlNode nodeObject = getNodeObject(tp);
				if (nodeObject.isScene) continue;
				LttlScene scene = Lttl.scenes.get(nodeObject.sceneId);
				LttlTransform transform = (LttlTransform) scene
						.findComponentById(nodeObject.id);
				if (transform == null) continue;
				list.add(transform);
			}
		}

		return list;
	}

	/**
	 * Returns all the transform ids that are selected in the tree.
	 * 
	 * @return
	 */
	public int[] getSelectedTransformIds()
	{
		int[] ids = new int[getSelectedTransformCount()];
		int i = 0;
		if (tree.getSelectionPaths() != null)
		{
			for (TreePath tp : tree.getSelectionPaths())
			{
				LttlNode nodeObject = getNodeObject(tp);
				if (nodeObject.isScene) continue;
				ids[i++] = nodeObject.id;
			}
		}

		return ids;
	}

	/**
	 * Returns a single selected transform, if more multi select or none, returns null
	 * 
	 * @return
	 */
	public LttlTransform getSelectedTransform()
	{
		if (getSelectedTransformCount() != 1) { return null; }

		return getSelectedTransforms().get(0);
	}

	public LttlScene getSelectedScene()
	{
		if (getSelectedTransformCount() == 0 && tree.getSelectionCount() == 1)
		{
			LttlNode nodeObject = getNodeObject(tree.getSelectionPath());
			if (nodeObject.isScene) { return Lttl.scenes.get(nodeObject.id); }
		}

		return null;
	}

	/**
	 * Adds scene transform tree, called on scene load
	 * 
	 * @param sceneId
	 */
	void addSceneTree(LttlScene scene)
	{
		if (Lttl.editor.getGui().disableGuiRefresh) { return; }

		DefaultMutableTreeNode newSceneNode = new DefaultMutableTreeNode(
				new LttlNode(scene));

		// create the scene node and add it to root at index
		getRoot().insert(newSceneNode, 0);

		// iterate through transform hierarchy and add each transform (recursively)
		for (LttlTransform lt : scene.getRef().transformHiearchy)
		{
			addTransform(lt, newSceneNode);
		}

		reloadModelMaintainExpand(getRoot());

		// make sure root is expanded
		tree.expandPath(getTreePath(getRoot()));
	}

	/**
	 * Removes scene transform tree, called on scene unload
	 * 
	 * @param scene
	 */
	void removeSceneTree(LttlScene scene)
	{
		if (Lttl.editor.getGui().disableGuiRefresh) { return; }

		getSceneNode(scene.getId()).removeFromParent();

		// clear selection if was selected
		if (tree.getSelectionCount() == 1)
		{
			LttlNode nodeObject = getNodeObject(tree.getSelectionPath());
			if (nodeObject.isScene)
			{
				tree.clearSelection();
			}
		}

		reloadModelMaintainExpand(getRoot());
	}

	/**
	 * Adds the provided transform to it's scene tree, and all of it's children.
	 * 
	 * @param transform
	 * @param scene
	 *            scene to add to
	 */
	void addTransform(LttlTransform transform, LttlSceneCore scene)
	{
		addTransform(transform, getSceneNode(scene.getId()));
	}

	/**
	 * removes this transform and all it's children from the scene tree specified.<br>
	 * Note this checks if parent is being destroy
	 * 
	 * @param transform
	 * @param scene
	 *            scene to add to, will throw error if no scene tree exists
	 */
	void removeTransform(LttlTransform transform, LttlSceneCore scene)
	{
		if (Lttl.editor.getGui().disableGuiRefresh) { return; }

		// skip if it is in a scene that is pending unload
		if (scene.isPendingUnload) { return; }

		// get parent node
		DefaultMutableTreeNode parentNode;
		DefaultMutableTreeNode sceneNode = getSceneNode(scene.getId());
		if (transform.getParent() != null)
		{
			parentNode = getTransformNode(transform.getParent(), sceneNode);
		}
		else
		{
			parentNode = sceneNode;
		}

		// clear selection if deleting a transform that is selected
		if (getSelectedTransforms().contains(transform))
		{
			clearSelection();
		}

		// remove transform node from tree
		reloadModelMaintainExpand(parentNode);

		// update selection and properties (may have been selected)
		valueChanged(null);
	}

	public void clearSelection()
	{
		tree.clearSelection();
	}

	/**
	 * Adds transform node to parent transform node
	 * 
	 * @param transform
	 * @param parentTransformNode
	 */
	private DefaultMutableTreeNode addTransformToParent(
			LttlTransform transform, DefaultMutableTreeNode parentTransformNode)
	{
		// add transform to parent node
		DefaultMutableTreeNode transformNode = new DefaultMutableTreeNode(
				new LttlNode(transform));
		parentTransformNode.add(transformNode);

		// add each child directly to parent node (since we already know it)
		for (LttlTransform child : transform.getChildren())
		{
			addTransformToParent(child, transformNode);
		}

		return transformNode;
	}

	/**
	 * Adds the provided transform to it's scene tree, and all of it's children.
	 * 
	 * @param transform
	 * @param sceneNode
	 */
	private void addTransform(LttlTransform transform,
			DefaultMutableTreeNode sceneNode)
	{
		if (Lttl.editor.getGui().disableGuiRefresh) { return; }

		// get parent node
		DefaultMutableTreeNode parentNode;
		if (transform.getParent() != null)
		{
			parentNode = getTransformNode(transform.getParent(), sceneNode);
		}
		else
		{
			parentNode = sceneNode;
		}

		// reload, which will add it in the process
		reloadModelMaintainExpand(parentNode);

		// update selection and properties (may have been selected)
		valueChanged(null);
	}

	private DefaultMutableTreeNode getTransformNode(LttlTransform transform)
	{
		return getTransformNode(transform, getSceneNode(transform.getSceneId()));
	}

	/**
	 * @param transform
	 * @param sceneNode
	 * @return the node for this transform, null if can't be found, probably already removed
	 */
	private DefaultMutableTreeNode getTransformNode(LttlTransform transform,
			DefaultMutableTreeNode sceneNode)
	{
		// get parent node
		DefaultMutableTreeNode parentNode;
		if (transform.getParent() != null)
		{
			parentNode = getTransformNode(transform.getParent(), sceneNode);
		}
		else
		{
			parentNode = sceneNode;
		}

		// iterate through all children in parent node looking for this transform
		for (int i = 0; i < parentNode.getChildCount(); i++)
		{
			DefaultMutableTreeNode node = getNode(parentNode, i);
			LttlNode nodeObject = getNodeObject(node);
			if (transform.getId() == nodeObject.id) { return node; }
		}

		Lttl.Throw("Could not find transform: " + transform.getName() + "["
				+ transform.getId() + "] in " + getNodeObject(parentNode).name);
		return null;
	}

	/**
	 * Stores info about each transform or scene node in the scene's tree
	 * 
	 * @author Josh
	 */
	class LttlNode
	{
		private boolean isRoot = false;
		public boolean isScene = false;
		public String name = "";
		public int id;
		public int sceneId;
		public boolean enabled = true;

		/**
		 * Only meant to be called by root
		 */
		LttlNode()
		{
			isRoot = true;
			name = "root";
		}

		LttlNode(LttlScene scene)
		{
			update(scene);
		}

		LttlNode(LttlTransform transform)
		{
			update(transform);
		}

		/**
		 * Updates node on reload
		 * 
		 * @param scene
		 * @return
		 */
		LttlNode update(LttlScene scene)
		{
			if (scene == Lttl.scenes.getWorld())
			{
				name = "_" + scene.getName();
			}
			else
			{
				name = scene.getName();
			}
			id = scene.getId();
			isScene = true;

			return this;
		}

		/**
		 * Updates node on reload
		 * 
		 * @param transform
		 * @return
		 */
		LttlNode update(LttlTransform transform)
		{
			if (transform.getName() != null && !transform.getName().isEmpty())
			{
				name = transform.getName();
			}

			id = transform.getId();
			sceneId = transform.getSceneId();
			isScene = false;
			enabled = transform.isEnabledSelf();

			return this;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public boolean isRoot()
		{
			return isRoot;
		}
	}

	@SuppressWarnings("serial")
	class LttlTreeCellRenderer extends DefaultTreeCellRenderer
	{
		// save default colors
		java.awt.Color sf = DefaultLookup.getColor(this, ui,
				"Tree.selectionForeground");
		java.awt.Color nsf = DefaultLookup.getColor(this, ui,
				"Tree.textForeground");

		// create disabled colors
		java.awt.Color disabledSF = new java.awt.Color(0, 1, 1, .7f);
		java.awt.Color disabledNSF = new java.awt.Color(1, 0, 0, .7f);

		public LttlTreeCellRenderer()
		{
			// had background
			setBackgroundNonSelectionColor(new java.awt.Color(1, 1, 1, 0));
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row,
				boolean hasFocus)
		{
			LttlNode nodeObject = getNodeObject((DefaultMutableTreeNode) value);

			// set the properties for component
			if (!nodeObject.isScene && !nodeObject.enabled)
			{
				// disabled
				setTextSelectionColor(disabledSF);
				setTextNonSelectionColor(disabledNSF);
			}
			else
			{
				// enabled
				setTextSelectionColor(sf);
				setTextNonSelectionColor(nsf);
			}

			// actually render component
			return super.getTreeCellRendererComponent(tree, value, sel,
					expanded, leaf, row, hasFocus);
		}
	}

	private DefaultMutableTreeNode getRoot()
	{
		return (DefaultMutableTreeNode) model.getRoot();
	}

	/**
	 * Returns the node object, could be a scene or transform from tree path
	 * 
	 * @param treePath
	 * @return
	 */
	private LttlNode getNodeObject(TreePath treePath)
	{
		return ((LttlNode) getNode(treePath).getUserObject());
	}

	private DefaultMutableTreeNode getNode(TreePath treePath)
	{
		return (DefaultMutableTreeNode) treePath.getLastPathComponent();
	}

	/**
	 * Returns the node object, could be a scene or transform from node
	 * 
	 * @param node
	 * @return
	 */
	private LttlNode getNodeObject(DefaultMutableTreeNode node)
	{
		return (LttlNode) node.getUserObject();
	}

	/**
	 * Returns node from parent based on index
	 * 
	 * @param parent
	 * @param index
	 * @return
	 */
	private DefaultMutableTreeNode getNode(DefaultMutableTreeNode parent,
			int index)
	{
		return (DefaultMutableTreeNode) parent.getChildAt(index);
	}

	private DefaultMutableTreeNode getSceneNode(int sceneId)
	{
		for (int i = 0; i < getRoot().getChildCount(); i++)
		{
			DefaultMutableTreeNode node = getNode(getRoot(), i);
			LttlNode nodeObject = getNodeObject(node);
			if (!nodeObject.isScene)
			{
				Lttl.Throw("Expected node to be a scene.");
			}

			// check if found correct scene
			if (nodeObject.id == sceneId) { return node; }
		}

		return null;
	}

	private TreePath getTreePath(DefaultMutableTreeNode node)
	{
		return new TreePath(model.getPathToRoot(node));
	}

	JPanel getPanel()
	{
		return panel;
	}

	/**
	 * Reloads the node, mainly the name.
	 * 
	 * @param transform
	 */
	void reloadNode(LttlTransform transform)
	{
		if (Lttl.editor.getGui().disableGuiRefresh) { return; }

		DefaultMutableTreeNode node = getTransformNode(transform);
		((LttlNode) node.getUserObject()).update(transform);
		reloadModelMaintainExpand(node);
	}

	/**
	 * Reloads the node, mainly for the name.
	 * 
	 * @param scene
	 */
	void reloadNode(LttlScene scene)
	{
		DefaultMutableTreeNode node = getSceneNode(scene.getId());
		// skip reloading node if it doesn't even exist yet, probably just created the scene
		if (node == null) return;

		((LttlNode) node.getUserObject()).update(scene);
		reloadModelMaintainExpand(node);
	}

	/**
	 * Reloads the node, sorts it, and maintains expanded nodes
	 * 
	 * @param node
	 */
	private void reloadModelMaintainExpand(DefaultMutableTreeNode node)
	{
		// save current selection
		TreePath[] selection = tree.getSelectionPaths();

		// get expanded node paths, but save their rows not paths, because the paths may change
		Enumeration<TreePath> etp = tree
				.getExpandedDescendants(getTreePath(getRoot()));
		IntArray expandedRows = new IntArray();
		if (etp != null)
		{
			while (etp.hasMoreElements())
			{
				TreePath tp = etp.nextElement();
				expandedRows.add(tree.getRowForPath(tp));
			}
		}

		// repopulate with nodes
		LttlNode parentNode = getNodeObject(node);

		if (!parentNode.isRoot())
		{
			node.removeAllChildren();

			List<LttlTransform> list;
			if (parentNode.isScene)
			{
				// get transform hierarchy list
				LttlScene parentScene = Lttl.scenes.get(parentNode.id);
				list = parentScene.getTopLevelTransforms();
			}
			else
			{
				// get the
				LttlTransform parentTransform = (LttlTransform) Lttl.scenes
						.findComponentByIdAllScenes(parentNode.id);
				list = parentTransform.getChildren();
			}

			for (LttlTransform child : list)
			{
				if (!child.isDestroyPending())
				{
					// adds in order based on list
					addTransformToParent(child, node);
				}
			}
		}

		model.reload(node);

		// expand previously expanded nodes
		for (int i = 0; i < expandedRows.size; i++)
		{
			tree.expandRow(expandedRows.get(i));
		}

		// reselect
		tree.setSelectionPaths(selection);
	}

	@Override
	public void treeExpanded(TreeExpansionEvent event)
	{
		// reloadModelMaintainExpand(getNode(event.getPath()));
	}

	@Override
	public void treeCollapsed(TreeExpansionEvent event)
	{
	}

	/**
	 * Saves selection ids
	 */
	void savePreviousSelectionIds()
	{
		// save selection
		prevSelectedSceneId = (getSelectedScene() == null) ? -1
				: getSelectedScene().getId();
		prevSelectedTransformId = (getSelectedTransformCount() == 1) ? getSelectedTransform()
				.getId() : -1;
	}

	/**
	 * Reselects based on previous mode.
	 */
	void reselect()
	{
		if (prevSelectedSceneId != -1)
		{
			if (Lttl.scenes.get(prevSelectedSceneId) != null)
			{
				setSelectedScene(Lttl.scenes.get(prevSelectedSceneId));
			}
		}
		else if (prevSelectedTransformId != -1)
		{
			setSelection((LttlTransform) Lttl.scenes
					.findComponentByIdAllScenes(prevSelectedTransformId));
		}
		prevSelectedSceneId = -1;
		prevSelectedTransformId = -1;
	}

	private long arrowKeyDownTime = 0;
	private IntMap<Vector2> intialPosArrowKey;

	/**
	 * draws the handles, outlines, and selection box transforms based on their render sizes, AFTER everything has
	 * changed this frame
	 */
	void debugDraw()
	{
		// draw highlighted transform
		if (highlightedTransform != null)
		{
			tmpColor.set(Lttl.editor.getSettings().highlightColor);
			tmpColor.a *= .5f;

			PolygonContainer highlightedTransformPolyCont = highlightedTransform
					.getSelectionPolygon();
			if (highlightedTransformPolyCont != null)
			{
				Lttl.debug.drawPolygonFilledOutline(
						highlightedTransformPolyCont, tmpColor,
						LttlDebug.WIDTH_SMALL * Lttl.debug.eF(),
						Lttl.editor.getSettings().highlightColor);
			}
			else
			{
				Lttl.debug.drawCircleFilledOutline(
						highlightedTransform.getWorldPosition(true).x,
						highlightedTransform.getWorldPosition(false).y,
						LttlDebug.RADIUS_MEDIUM * Lttl.debug.eF(), tmpColor,
						Lttl.editor.getSettings().highlightColor);
			}
		}

		// update the camera handles
		updateCameraHandles();

		if (getSelectedTransformCount() == 1)
		{
			// Draw single selection boundary box
			LttlTransform lt = getSelectedTransform();
			drawSelectedTransform(lt, false);

			// may have changed from editor interface or from arrow keys
			updateHandlePositions();

		}
		else if (getSelectedTransformCount() > 1)
		{
			if (Lttl.editor.getSettings().showSelectionOutline)
			{
				// Draw multi selection boundary box
				Lttl.debug.tmpColor
						.set(Lttl.editor.getSettings().colorMultiSelect);
				Lttl.debug.tmpColor.a = .1f;
				Lttl.debug.drawRectFilledOutline(multiSelectRectangle, 0,
						Lttl.debug.tmpColor, LttlDebug.WIDTH_MEDIUM
								* Lttl.debug.eF(),
						Lttl.editor.getSettings().colorMultiSelect);
			}
			for (LttlTransform lt : getSelectedTransforms())
			{
				drawSelectedTransform(lt, true);
			}

			// may have changed from editor interface or from arrow keys
			updateHandlePositions();
		}

		// Draw select box
		if (selectBoxRectangle != null)
		{
			tmpColor.set(Lttl.editor.getSettings().colorMultiSelect);
			tmpColor.a *= .3f;
			Lttl.debug.drawRectFilledOutline(selectBoxRectangle, 0, tmpColor,
					0, Lttl.editor.getSettings().colorMultiSelect);
		}

		// Draw zoom box rect
		if (zoomBoxRectangle != null)
		{
			tmpColor.set(Lttl.editor.getSettings().colorZoomBoxRect);
			tmpColor.a *= .3f;
			Lttl.debug.drawRectFilledOutline(zoomBoxRectangle, 0, tmpColor, 0,
					Lttl.editor.getSettings().colorZoomBoxRect);
		}
	}

	/**
	 * Updates based on some inputs
	 */
	void update()
	{
		if (!handlesUpdatedAtleastOnce && Lttl.game.getFrameCount() > 1)
		{
			updateHandles();
		}

		// check if using arrow keys to adjust position
		if (getSelectedTransformCount() > 0)
		{
			// key was pressed this frame then shift it
			if (Lttl.input.isEditorAnyKeyPressed(Keys.LEFT, Keys.RIGHT,
					Keys.DOWN, Keys.UP))
			{
				// save undo values
				intialPosArrowKey = new IntMap<Vector2>(
						getSelectedTransformCount());
				for (LttlTransform lt : getSelectedTransforms())
				{
					intialPosArrowKey.put(lt.getId(), new Vector2(lt.position));
				}

				arrowKeyDownTime = System.currentTimeMillis();
				processArrowKeyShift();

			}

			// if key was held for a period of time, then slide it
			if (Lttl.input.isEditorAnyKeyDown(Keys.LEFT, Keys.RIGHT, Keys.DOWN,
					Keys.UP))
			{
				if (arrowKeyDownTime + 500 <= System.currentTimeMillis())
				{
					processArrowKeyShift();
				}
			}

			// if key was released then save undo
			if (Lttl.input.isEditorAnyKeyReleased(Keys.LEFT, Keys.RIGHT,
					Keys.DOWN, Keys.UP))
			{
				// create undo
				ArrayList<UndoField> undoFields = new ArrayList<UndoField>(
						getSelectedTransformCount());
				for (LttlTransform lt : getSelectedTransforms())
				{
					if (!intialPosArrowKey.containsKey(lt.getId())) continue;

					undoFields.add(new UndoField(lt, intialPosArrowKey.get(lt
							.getId()), new Vector2(lt.position),
							UndoSetter.TransformPosition));
				}
				Lttl.editor
						.getUndoManager()
						.registerUndoState(
								new UndoState(
										((getSelectedTransformCount() == 1) ? getSelectedTransform()
												.getName() : "MutliSelect")
												+ " - Position Nudge",
										undoFields));
			}
		}
	}

	private void updateHandlePositions()
	{
		if (handlePos != null)
		{
			handlePos.position.set(getSelectedTransform()
					.getWorldPosition(true));
		}
		if (handleRot != null)
		{
			handleRot.position.set(getSelectedTransform()
					.getWorldPosition(true));
		}
		if (handleScale != null)
		{
			handleScale.position.set(getSelectedTransform().getWorldPosition(
					true));
		}
		if (handleMultiPos != null)
		{
			multiSelectRectangle.getCenter(handleMultiPos.position);
		}
		if (handleMultiScale != null)
		{
			multiSelectRectangle.getCenter(handleMultiScale.position);
		}
	}

	private void processArrowKeyShift()
	{
		float stepSize = Lttl.editor.getSettings().arrowKeyStepFactor
				* Lttl.game.getSettings().getWidthFactor()
				* ((Lttl.input.isEditorShift()) ? 10 : 1);
		if (Lttl.input.isEditorKeyDown(Keys.LEFT))
		{
			shiftSelection(-stepSize, 0);
			if (getSelectedTransformCount() > 1)
			{
				multiSelectRectangle.x -= stepSize;
			}
		}
		else if (Lttl.input.isEditorKeyDown(Keys.RIGHT))
		{
			shiftSelection(stepSize, 0);
			if (getSelectedTransformCount() > 1)
			{
				multiSelectRectangle.x += stepSize;
			}
		}
		else if (Lttl.input.isEditorKeyDown(Keys.UP))
		{
			shiftSelection(0, stepSize);
			if (getSelectedTransformCount() > 1)
			{
				multiSelectRectangle.y += stepSize;
			}
		}
		else if (Lttl.input.isEditorKeyDown(Keys.DOWN))
		{
			shiftSelection(0, -stepSize);
			if (getSelectedTransformCount() > 1)
			{
				multiSelectRectangle.y -= stepSize;
			}
		}
	}

	private void shiftSelection(float shiftX, float shiftY)
	{
		for (LttlTransform lt : getSelectedTransforms())
		{
			lt.updateWorldValues();
			tmpV2.set(lt.getWorldPosition(false).x + shiftX,
					lt.getWorldPosition(false).y + shiftY);
			lt.setWorldPosition(tmpV2);
			lt.onGuiChange(LttlTransformChangeType.Position, false, lt);
		}
	}

	private void drawSelectedTransform(LttlTransform lt, boolean inMultiSelect)
	{
		// draw outline
		if (Lttl.editor.getSettings().showSelectionOutline)
		{
			// if has selection polygon, then draw that
			float[] selectionPolygonBoundingRectTransformed = lt
					.getSelectionTransformedBoundingRect();
			if (selectionPolygonBoundingRectTransformed != null)
			{
				Lttl.debug.drawPolygonOutline(
						selectionPolygonBoundingRectTransformed,
						LttlDebug.WIDTH_SMALL * Lttl.debug.eF(),
						prepColorDrawOutline(inMultiSelect));
			}
			else
			{
				// draw a rect around each transform tree selected if no renderer, if has renderer around the mesh
				tempRect = lt.getSelectionAABBTree(tempRect, true);
				// check to see if rect has any width
				if (tempRect.width > 0)
				{
					Lttl.debug.drawRectOutline(tempRect, LttlDebug.WIDTH_SMALL
							* Lttl.debug.eF(),
							prepColorDrawOutline(inMultiSelect));
				}
			}
		}

		// draw hiearchy tree (lines that connect parents to children)
		if (Lttl.editor.getSettings().showTransformHiearchy)
		{
			LttlTransform highest = lt.getHighestParent();
			highest.updateWorldValuesTree();
			drawHiearchyTree(highest, 0);
		}
	}

	private void drawHiearchyTree(LttlTransform transform, int level)
	{
		Color color = hiearchyTreeColors.get(level % hiearchyTreeColors.size());
		// all children and parent are assumed to have updated world values because of updateWorldValuesTree ran earlier
		for (LttlTransform child : transform.children)
		{
			Lttl.debug.drawLine(transform.getWorldPosition(false).x,
					transform.getWorldPosition(false).y,
					child.getWorldPosition(false).x,
					child.getWorldPosition(false).y, 0, color);
			drawHiearchyTree(child, level + 1);
		}
	}

	private Color prepColorDrawOutline(boolean inMultiSelect)
	{
		return (inMultiSelect) ? tmpColor.set(
				Lttl.editor.getSettings().colorSelect.r,
				Lttl.editor.getSettings().colorSelect.g,
				Lttl.editor.getSettings().colorSelect.b,
				Lttl.editor.getSettings().colorSelect.a * .5f) : Lttl.editor
				.getSettings().colorSelect;
	}

	private boolean handlesUpdatedAtleastOnce = false;

	/**
	 * Selection has changed, create all new handles
	 */
	void updateHandles()
	{
		handlesUpdatedAtleastOnce = true;

		ArrayList<LttlTransform> selected = getSelectedTransforms();

		// null and unregister previous all handles
		if (handlePos != null)
		{
			handlePos.unregister();
			handlePos = null;
		}
		if (handleRot != null)
		{
			handleRot.unregister();
			handleRot = null;
		}
		if (handleScale != null)
		{
			handleScale.unregister();
			handleScale = null;
		}
		if (handleMultiPos != null)
		{
			handleMultiPos.unregister();
			handleMultiPos = null;
		}
		if (handleMultiScale != null)
		{
			handleMultiScale.unregister();
			handleMultiScale = null;
		}

		if (selected.size() == 1)
		{
			// single selected
			final LttlTransform lt = selected.get(0);
			lt.updateWorldValues();

			// no renderer transform
			if ((lt.r() == null || lt.r().getMesh() == null || LttlMultiRenderer.class
					.isAssignableFrom(lt.r().getClass()))
					&& Lttl.editor.getGui().getStatusBarController().handlePosButton
							.isSelected())
			{
				// POSITION HANDLE
				handlePos = new HandleRect(lt.getWorldPosition(false).x,
						lt.getWorldPosition(false).y, 0,
						Lttl.editor.getSettings().handleSize,
						Lttl.editor.getSettings().handleSize, true, 0, true,
						true, true, false,
						Lttl.editor.getSettings().handlePosColor, 0, null)
				{
					private Vector2 initialPos;

					@Override
					public void onPressed()
					{
						initialPos = new Vector2(lt.position);
					}

					@Override
					public void onDrag(float deltaX, float deltaY)
					{
						// update position of transform
						shiftSelection(deltaX, deltaY);
					}

					@Override
					public void onReleased()
					{
						if (Lttl.quiet(initialPos)) return;

						// create undo
						Lttl.editor.getUndoManager().registerUndoState(
								new UndoState(lt.getName()
										+ " - Handle Position", new UndoField(
										lt, new Vector2(initialPos),
										new Vector2(lt.position),
										UndoSetter.TransformPosition)));
					}
				};
			}
			else if (Lttl.editor.getGui().getStatusBarController().handleRotButton
					.isSelected())
			{
				// ROTATION HANDLE
				handleRot = new HandleRect(lt.getWorldPosition(false).x,
						lt.getWorldPosition(false).y, 0,
						Lttl.editor.getSettings().handleSize,
						Lttl.editor.getSettings().handleSize, true, 0, true,
						false, false, false,
						Lttl.editor.getSettings().handleRotColor, 0, null)
				{
					private float startingLocalRotation = 0;
					private float currentRotation = 0;
					private float undoValue;

					@Override
					public void onPressed()
					{
						// save undo value
						undoValue = lt.rotation;

						startingLocalRotation = lt.rotation;
						currentRotation = startingLocalRotation;
					}

					@Override
					public void onDrag(float deltaX, float deltaY)
					{
						currentRotation -= (Lttl.input.getPixelDeltaX() / Lttl.editor
								.getSettings().handleRotationSmoothness);
						if (Lttl.editor.getInput().isShiftEV())
						{
							float leftOver = (currentRotation % 360) % 45;
							lt.rotation = currentRotation + (45 - leftOver);
						}
						else
						{
							lt.rotation = currentRotation;
						}

						lt.onGuiChange(LttlTransformChangeType.Rotation, true,
								lt);
					}

					@Override
					public void onReleased()
					{
						// create undo
						Lttl.editor.getUndoManager().registerUndoState(
								new UndoState(lt.getName()
										+ " - Handle Rotation", new UndoField(
										lt, this.undoValue, lt.rotation,
										UndoSetter.TransformRotation)));
					}
				};
			}
			else if (Lttl.editor.getGui().getStatusBarController().handleSclButton
					.isSelected())
			{
				// SCALE HANDLE
				handleScale = new HandleRect(lt.getWorldPosition(false).x,
						lt.getWorldPosition(false).y, 0,
						Lttl.editor.getSettings().handleSize,
						Lttl.editor.getSettings().handleSize, true, 0, true,
						false, true, false,
						Lttl.editor.getSettings().handleSclColor, 0, null)
				{
					private float scaleFactorX = 1;
					private float scaleFactorY = 1;
					private Vector2 initialScale = new Vector2();

					@Override
					public void onPressed()
					{
						scaleFactorX = 1;
						scaleFactorY = 1;
						initialScale.set(lt.scale);
					}

					@Override
					public void onDrag(float deltaX, float deltaY)
					{
						if (!this.canLockAxis
								|| this.getLockedAxis() != LockedAxis.Y)
						{
							if (Lttl.input.getEditorX() <= this.position.x)
							{
								scaleFactorX += deltaX
										/ Lttl.editor.getSettings().handleScaleSmoothness
										/ Lttl.editor.getSettings().handleScaleDecreasingSmoothness
										* Lttl.editor.getCamera().zoom;
							}
							else
							{
								scaleFactorX += deltaX
										/ Lttl.editor.getSettings().handleScaleSmoothness
										* Lttl.editor.getCamera().zoom;
							}
						}
						else
						{
							if (Lttl.input.getEditorX() <= this.position.y)
							{
								scaleFactorY += deltaY
										/ Lttl.editor.getSettings().handleScaleSmoothness
										/ Lttl.editor.getSettings().handleScaleDecreasingSmoothness
										* Lttl.editor.getCamera().zoom;
							}
							else
							{
								scaleFactorY += deltaY
										/ Lttl.editor.getSettings().handleScaleSmoothness
										* Lttl.editor.getCamera().zoom;
							}
						}
						if (!this.canLockAxis
								|| this.getLockedAxis() == LockedAxis.None)
						{
							scaleFactorY = scaleFactorX;
						}
						lt.scale.set(initialScale.x * scaleFactorX,
								initialScale.y * scaleFactorY);
						lt.onHandleScaleDrag();
						lt.onGuiChange(LttlTransformChangeType.Scale, true, lt);
					}

					@Override
					public void onReleased()
					{
						// create undo
						Lttl.editor.getUndoManager().registerUndoState(
								new UndoState(lt.getName() + " - Handle Scale",
										new UndoField(lt, new Vector2(
												initialScale), new Vector2(
												lt.scale),
												UndoSetter.TransformScale)));
					}
				};
			}
		}
		else if (selected.size() > 1)
		{
			// multiple selected

			// calculate group rect
			final ArrayList<LttlTransform> transforms = getSelectedTransforms();
			FloatArray points = new FloatArray(transforms.size() * 6); // estimate
			for (LttlTransform lt : transforms)
			{
				lt.updateWorldValues();
				points.addAll(lt.getSelectionBoundingRectPointsTree(true));
			}
			multiSelectRectangle = LttlMath.GetAABB(points.toArray(),
					multiSelectRectangle);
			final Vector2 center = multiSelectRectangle
					.getCenter(new Vector2());

			if (Lttl.editor.getGui().getStatusBarController().handlePosButton
					.isSelected())
			{
				// POSITION HANDLE
				handleMultiPos = new HandleRect(center.x, center.y, 0,
						Lttl.editor.getSettings().handleSize,
						Lttl.editor.getSettings().handleSize, true, 0, true,
						true, true, false,
						Lttl.editor.getSettings().handlePosColor, 0, null)
				{
					private IntMap<Vector2> undoPosMap = new IntMap<Vector2>();

					@Override
					public void onPressed()
					{
						for (LttlTransform lt : getSelectedTransforms())
						{
							undoPosMap
									.put(lt.getId(), new Vector2(lt.position));
						}
					}

					@Override
					public void onReleased()
					{
						// get redo values
						IntMap<Vector2> redoPosMap = new IntMap<Vector2>();
						for (LttlTransform lt : transforms)
						{
							redoPosMap
									.put(lt.getId(), new Vector2(lt.position));
						}

						ArrayList<UndoField> undoFields = new ArrayList<UndoField>();
						for (LttlTransform lt : transforms)
						{
							undoFields.add(new UndoField(lt, undoPosMap.get(lt
									.getId()), redoPosMap.get(lt.getId()),
									UndoSetter.TransformPosition));
						}

						// create undo
						Lttl.editor.getUndoManager().registerUndoState(
								new UndoState("Multiselect - Handle Position",
										undoFields));
					}

					@Override
					public void onDrag(float deltaX, float deltaY)
					{
						shiftSelection(deltaX, deltaY);

						// adjust rectangle too
						multiSelectRectangle.x += deltaX;
						multiSelectRectangle.y += deltaY;
					}
				};
			}
			else if (Lttl.editor.getGui().getStatusBarController().handleSclButton
					.isSelected())
			{
				// SCALE HANDLE
				handleMultiScale = new HandleRect(center.x, center.y, 0,
						Lttl.editor.getSettings().handleSize,
						Lttl.editor.getSettings().handleSize, true, 0, true,
						false, false, false,
						Lttl.editor.getSettings().handleSclColor, 0, null)
				{
					private float scaleFactor;
					private IntMap<Vector2> intialScaleMap = new IntMap<Vector2>(
							transforms.size());
					private IntMap<Vector2> intialCenterOffsetsMap = new IntMap<Vector2>(
							transforms.size());
					private IntMap<Vector2> intialPosMap = new IntMap<Vector2>(
							transforms.size());
					private Vector2 initalBoundingRectDimensions = new Vector2();

					@Override
					public void onPressed()
					{
						// get inital properties to scale from
						for (LttlTransform lt : transforms)
						{
							intialScaleMap.put(lt.getId(),
									new Vector2(lt.scale));
							intialPosMap.put(lt.getId(), new Vector2(
									lt.position));
							intialCenterOffsetsMap.put(lt.getId(), new Vector2(
									lt.getWorldPosition(true)).sub(center));
						}

						// save group bounding rect dimensions
						initalBoundingRectDimensions.set(
								multiSelectRectangle.width,
								multiSelectRectangle.height);

						scaleFactor = 1;
					}

					@Override
					public void onDrag(float deltaX, float deltaY)
					{
						scaleFactor += (deltaX + Lttl.input.getEditorDeltaY())
								/ Lttl.editor.getSettings().handleScaleSmoothness;
						for (LttlTransform lt : transforms)
						{
							// scale
							lt.scale.set(intialScaleMap.get(lt.getId()).x
									* scaleFactor,
									intialScaleMap.get(lt.getId()).y
											* scaleFactor);

							// position scaled
							lt.setWorldPosition(tmpV2
									.set(intialCenterOffsetsMap.get(lt.getId()))
									.scl(scaleFactor).add(center));

							lt.onHandleScaleDrag();
							lt.onGuiChange(LttlTransformChangeType.Scale, true,
									lt);
						}

						// update multiSelectRectangle
						multiSelectRectangle.width = initalBoundingRectDimensions.x
								* scaleFactor;
						multiSelectRectangle.height = initalBoundingRectDimensions.y
								* scaleFactor;
						multiSelectRectangle.setCenter(center);
					}

					@Override
					public void onReleased()
					{
						// get redo values
						IntMap<Vector2> redoPosMap = new IntMap<Vector2>();
						for (LttlTransform lt : transforms)
						{
							redoPosMap
									.put(lt.getId(), new Vector2(lt.position));
						}
						IntMap<Vector2> redoSclMap = new IntMap<Vector2>();
						for (LttlTransform lt : transforms)
						{
							redoSclMap.put(lt.getId(), new Vector2(lt.scale));
						}

						ArrayList<UndoField> undoFields = new ArrayList<UndoField>();
						for (LttlTransform lt : transforms)
						{
							undoFields.add(new UndoField(lt, intialPosMap
									.get(lt.getId()),
									redoPosMap.get(lt.getId()),
									new UndoSetter()
									{
										@Override
										public void set(LttlComponent comp,
												Object value)
										{
											((LttlTransform) comp).position
													.set((Vector2) value);
										}
									}));
							undoFields.add(new UndoField(lt, intialScaleMap
									.get(lt.getId()),
									redoSclMap.get(lt.getId()),
									new UndoSetter()
									{
										@Override
										public void set(LttlComponent comp,
												Object value)
										{
											((LttlTransform) comp).scale
													.set((Vector2) value);
										}
									}));
						}

						// create undo
						Lttl.editor.getUndoManager().registerUndoState(
								new UndoState("Multiselect - Handle Scale",
										undoFields));
					}
				};
			}
		}
		else
		{
			// no selection
		}
	}

	private class TransformBounds
	{
		Rectangle aabb;
		PolygonContainer polygonCont;
	}

	private ArrayList<LttlTransform> initialSelectedTransforms = null;
	private HashMap<LttlTransform, TransformBounds> boundsCache = new HashMap<LttlTransform, TransformBounds>();
	private ArrayList<LttlTransform> selectBoxTransforms = new ArrayList<LttlTransform>();
	LttlTransform pressedOnNonSelected = null;
	private Vector2 intialSingleSelectPos = null;

	void processLeftClickDrag(PressedType pressedType, Vector2 mouseDownPosition)
	{
		final ArrayList<LttlTransform> selectedTransforms = Lttl.editor
				.getGui().getSelectionController().getSelectedTransforms();

		switch (pressedType)
		{
			case OnNonSelectedTransform:
				setSelection(pressedOnNonSelected);
				pressedType = PressedType.OnSelectedTransform;
			case OnSelectedTransform:
				if (selectedTransforms.size() == 1)
				{
					LttlTransform lt = getSelectedTransform();

					// save undo value
					if (intialSingleSelectPos == null)
					{
						intialSingleSelectPos = new Vector2(lt.position);
					}

					// update position of single transform
					shiftSelection(
							(Lttl.editor.getInput().getLockedAxis() == LockedAxis.None || Lttl.editor
									.getInput().getLockedAxis() == LockedAxis.X) ? Lttl.input
									.getEditorDeltaX() : 0,
							(Lttl.editor.getInput().getLockedAxis() == LockedAxis.None || Lttl.editor
									.getInput().getLockedAxis() == LockedAxis.Y) ? Lttl.input
									.getEditorDeltaY() : 0);

					// update handle positions
					if (handleRot != null)
					{
						handleRot.position.set(tmpV2);
					}
					if (handleScale != null)
					{
						handleScale.position.set(tmpV2);
					}

					if (Lttl.input.isEitherMouseReleased(0))
					{
						// create undo
						Lttl.editor.getUndoManager().registerUndoState(
								new UndoState(lt.getName()
										+ " - Handle Position", new UndoField(
										lt, intialSingleSelectPos, new Vector2(
												lt.position),
										UndoSetter.TransformPosition)));
						intialSingleSelectPos = null;
					}
				}
				break;
			case OnNothing:
				selectBoxTransforms.clear();

				// if mouse was released this frame then remove select box
				if (Lttl.input.isEitherMouseReleased(0))
				{
					// tells properties panel you can draw now
					selectBoxRectangle = null;

					// update the selection tree so it updates properties panel, since it's not updateing during
					// selectbox drawing
					valueChanged(null);

					initialSelectedTransforms = null;
					boundsCache.clear();
				}
				else if (Lttl.input.isEitherMouseDown(0))
				{
					// first time stuff
					// create rect
					if (selectBoxRectangle == null)
					{
						selectBoxRectangle = new Rectangle();
					}
					// save the initial selected transforms
					if (initialSelectedTransforms == null)
					{
						initialSelectedTransforms = new ArrayList<LttlTransform>(
								selectedTransforms);
					}

					// update select box size
					selectBoxRectangle.width = LttlMath.abs(mouseDownPosition.x
							- Lttl.input.getEditorX());
					selectBoxRectangle.height = LttlMath
							.abs(mouseDownPosition.y - Lttl.input.getEditorY());
					if (Lttl.input.getEditorX() < mouseDownPosition.x)
					{
						selectBoxRectangle.x = Lttl.input.getEditorX();
					}
					else
					{
						selectBoxRectangle.x = Lttl.input.getEditorX()
								- selectBoxRectangle.width;
					}
					if (Lttl.input.getEditorY() < mouseDownPosition.y)
					{
						selectBoxRectangle.y = Lttl.input.getEditorY();
					}
					else
					{
						selectBoxRectangle.y = Lttl.input.getEditorY()
								- selectBoxRectangle.height;
					}

					// create new geometry every single drag frame, but it's just a rect so okay
					com.vividsolutions.jts.geom.Polygon selectBoxPolygon = LttlGeometryUtil
							.createPolygon(selectBoxRectangle);

					// a function that takes a rectange and returns selection (no decendants)
					// The select box works efficiently by caching, so if an object is moving, it won't work
					for (LttlTransform lt : Lttl.scenes
							.findComponentsAllScenes(LttlTransform.class, false))
					{
						// skip if unselectable or disabled
						if (!lt.isEnabled() || !GuiHelper.isSelectable(lt))
						{
							continue;
						}

						lt.updateWorldValues();

						// see if already have cached this transform
						// boundsCache only holes LttlTransforms with selection functionality
						TransformBounds tbs = boundsCache.get(lt);

						// new, add it to cache
						if (tbs == null)
						{
							// create cache of transfomed axis align bounding rect
							Rectangle aabb = lt
									.getSelectionBoundingRectTransformedAxisAligned();
							// skip transform that don't have selection functionality
							if (aabb == null)
							{
								continue;
							}

							tbs = new TransformBounds();
							tbs.aabb = aabb;
							boundsCache.put(lt, tbs);
						}

						// if it overlaps with the outer bounding rect, then check mesh points
						if (tbs.aabb.overlaps(selectBoxRectangle))
						{
							// create cache of polygon
							if (tbs.polygonCont == null)
							{
								tbs.polygonCont = lt.getSelectionPolygon();
							}

							// check if polygon and select box intersect
							if (selectBoxPolygon.intersects(tbs.polygonCont
									.getPolygon()))
							{
								addSelectBoxTransform(lt);
							}
						}
					}

					ArrayList<LttlTransform> newSelection = new ArrayList<LttlTransform>();
					// difference
					if (Lttl.editor.getInput().isControlEV())
					{
						// create a list of the new selection transforms (starts with initial selection transforms)
						newSelection.addAll(initialSelectedTransforms);
						for (LttlTransform lt : selectBoxTransforms)
						{
							if (initialSelectedTransforms.contains(lt))
							{
								newSelection.remove(lt);
							}
							else
							{
								newSelection.add(lt);
							}
						}
					}
					// cumulative
					else if (Lttl.editor.getInput().isShiftEV())
					{
						newSelection.addAll(selectBoxTransforms);
						newSelection.addAll(initialSelectedTransforms);
						LttlHelper
								.ArrayListRemoveDuplicates(newSelection, true);
					}
					// fresh selection
					else
					{
						newSelection.addAll(selectBoxTransforms);
					}

					// remove descendants
					LttlObjectHelper.RemoveDescendants(newSelection);

					// only set selection if it's different
					if (selectedTransforms.size() != newSelection.size()
							|| !selectedTransforms.containsAll(newSelection))
					{
						setSelection(newSelection);
					}
				}
				break;
			case ZoomRect:
				if (!Lttl.editor.getInput().isControlEV()
						|| !Lttl.editor.getInput().isShiftEV())
				{
					zoomBoxRectangle = null;
					return;
				}

				if (Lttl.input.isEitherMouseDown(0))
				{
					// first time stuff
					// create rect
					if (zoomBoxRectangle == null)
					{
						zoomBoxRectangle = new Rectangle();
					}

					// update select box size
					zoomBoxRectangle.width = LttlMath.abs(mouseDownPosition.x
							- Lttl.input.getEditorX());
					zoomBoxRectangle.height = LttlMath.abs(mouseDownPosition.y
							- Lttl.input.getEditorY());
					if (Lttl.input.getEditorX() < mouseDownPosition.x)
					{
						zoomBoxRectangle.x = Lttl.input.getEditorX();
					}
					else
					{
						zoomBoxRectangle.x = Lttl.input.getEditorX()
								- zoomBoxRectangle.width;
					}
					if (Lttl.input.getEditorY() < mouseDownPosition.y)
					{
						zoomBoxRectangle.y = Lttl.input.getEditorY();
					}
					else
					{
						zoomBoxRectangle.y = Lttl.input.getEditorY()
								- zoomBoxRectangle.height;
					}
				}
				else if (Lttl.input.isEitherMouseReleased(0))
				{
					Lttl.editor.getCamera().lookAt(
							LttlMath.GetRectFourCorners(zoomBoxRectangle),
							EaseType.QuadOut, .7f, 0);

					// clear rect
					zoomBoxRectangle = null;
				}
				break;
		}
	}

	private void addSelectBoxTransform(LttlTransform lt)
	{
		LttlTransform selectionGroup = GuiHelper.getSelectionGroupTransform(lt);
		if (selectionGroup != null)
		{
			lt = selectionGroup;
		}
		selectBoxTransforms.add(lt);
	}

	/**
	 * Checks to see if the editor mouse is in any meshes (or collider) and returns the transforms in order of z
	 * position, least to greatest
	 * 
	 * @param useRules
	 *            when right clicking this should be false
	 * @param position
	 *            world position to test, if null will check current editor mouse position
	 * @return
	 */
	public ArrayList<LttlTransform> checkSelectTransformsAtPosition(
			final boolean useRules, Vector2 position)
	{
		ArrayList<LttlTransform> list = new ArrayList<LttlTransform>();

		// check meshes
		Point point = LttlGeometryUtil.getGeometryFactory().createPoint(
				new Coordinate((position == null) ? Lttl.input.getEditorX()
						: position.x, (position == null) ? Lttl.input
						.getEditorY() : position.y));

		for (LttlTransform lt : Lttl.scenes.findComponentsAllScenes(
				LttlTransform.class, false))
		{
			// check if allowed to be selectable, then check if mouse is in it
			if (!lt.isEnabled()
					|| !(!useRules || GuiHelper.isSelectable(lt.transform())))
				continue;

			Rectangle aabb = lt
					.getSelectionBoundingRectTransformedAxisAligned();
			if (aabb == null) continue;

			if (!aabb.contains((float) point.getX(), (float) point.getY()))
				continue;

			Polygon p = lt.getSelectionPolygon().getPolygon();
			if (p == null) continue;

			// check if point is in polygon
			if (p.intersects(point))
			{
				LttlTransform transform = lt;
				if (useRules)
				{
					// check if there is a selectionGroup set on self or any ancestors
					LttlTransform selectionGroup = GuiHelper
							.getSelectionGroupTransform(transform);
					if (selectionGroup != null)
					{
						transform = selectionGroup;
					}
				}

				// add the transform if it's new
				if (!list.contains(transform))
				{
					list.add(transform);
				}
			}
		}

		// Sort the list based on index of the the transformsOrdered list
		Collections.sort(list, new Comparator<LttlTransform>()
		{
			@Override
			public int compare(LttlTransform o1, LttlTransform o2)
			{
				if (Lttl.loop.transformsOrdered.indexOf(o2) > Lttl.loop.transformsOrdered
						.indexOf(o1))
				{
					return 1;
				}
				else
				{
					return -1;
				}
			}
		});

		return list;
	}

	void createRightClickMenu()
	{
		editorPopupMenu = new JPopupMenu();

		/* Additional Menus for this frame */
		for (JMenuItem mi : Lttl.editor.getGui().rightClickMenuItems)
		{
			editorPopupMenu.add(mi);
		}
		if (Lttl.editor.getGui().rightClickMenuItems.size() > 0)

		{
			editorPopupMenu.add(new JSeparator());
		}
		Lttl.editor.getGui().rightClickMenuItems.clear();

		/* STANDARD MENU ITEMS */
		// check if mouse position was in a mesh
		final ArrayList<LttlTransform> focusedTransforms = new ArrayList<LttlTransform>();

		// decide the focused transform or transforms based on their outline
		if (getSelectedTransformCount() == 1)
		{
			LttlTransform lt = getSelectedTransform();

			// if there is a selection polygon bounding rect for this transform, then just check that
			float[] transformedBoundingRect = lt
					.getSelectionTransformedBoundingRect();
			if (transformedBoundingRect != null)
			{
				if (LttlGeometry.ContainsPointInPolygon(
						transformedBoundingRect, Lttl.input.getEditorX(),
						Lttl.input.getEditorY()))
				{
					focusedTransforms.add(lt);
				}
			}
			else
			{
				// if there is no selection polygon, then get the selection axis aligned bounding box for whole tree
				lt.updateWorldValuesTree();
				if (lt.getSelectionAABBTree(null, true).contains(
						Lttl.input.getEditorMousePos()))
				{
					focusedTransforms.add(lt);
				}
			}
		}
		else if (getSelectedTransformCount() > 1)
		{
			if (multiSelectRectangle.contains(Lttl.input.getEditorMousePos()))
			{
				focusedTransforms.addAll(getSelectedTransforms());
			}
		}
		// Select Menu
		final ArrayList<LttlTransform> clickedTransforms = checkSelectTransformsAtPosition(
				false, null);
		final JMenu selectMenu = new JMenu("Select");
		for (final LttlTransform transform : clickedTransforms)
		{
			JMenuItem menuItem = new JMenuItem(transform.getName());
			menuItem.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					selectionWithModifiers(transform);
				}
			});
			menuItem.addChangeListener(new ChangeListener()
			{
				private boolean selected = false;

				@Override
				public void stateChanged(ChangeEvent e)
				{
					selected = !selected;
					highlightedTransform = (selected) ? transform : null;
				}
			});
			selectMenu.add(menuItem);
		}
		selectMenu.setEnabled(clickedTransforms.size() > 0);
		editorPopupMenu.add(selectMenu);

		// Look At Menu Item
		final JMenuItem lookAtMenuItem = new JMenuItem("Look At");
		lookAtMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getCamera().lookAt(focusedTransforms,
						EaseType.QuadOut, 1f);
			}
		});
		lookAtMenuItem.setEnabled(focusedTransforms.size() > 0);
		editorPopupMenu.add(lookAtMenuItem);

		// Parent Menu Item
		final JMenuItem parentMenuItem = new JMenuItem("Parent");
		if (focusedTransforms.size() == 1)
		{
			parentMenuItem
					.setEnabled(focusedTransforms.get(0).getParent() != null);
		}
		else
		{
			parentMenuItem.setEnabled(false);
		}
		parentMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				selectionWithModifiers(focusedTransforms.get(0).getParent());
			}
		});
		parentMenuItem.addChangeListener(new ChangeListener()
		{
			private boolean selected = false;

			@Override
			public void stateChanged(ChangeEvent e)
			{
				selected = !selected;
				highlightedTransform = (selected) ? focusedTransforms.get(0)
						.getParent() : null;
			}
		});
		editorPopupMenu.add(parentMenuItem);

		editorPopupMenu.addPopupMenuListener(new PopupMenuListener()
		{

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
				// maintains focus on canvas after popupmenu disappears, this allows mouse wheel zoom functionaility to
				// continue
				Lttl.editor.getGui().focusCanvas();
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e)
			{
			}
		});

		// show editor menu
		editorPopupMenu
				.show(Lttl.editor.getGui().canvasPanel,
						LttlMath.round(Lttl.editor.getGui().canvasPanel
								.getWidth()
								* (Lttl.input.getRawPixelX() / Gdx.graphics
										.getWidth())),
						LttlMath.round(Lttl.editor.getGui().canvasPanel
								.getHeight()
								* (1 - (Lttl.input.getRawPixelY() / Gdx.graphics
										.getHeight()))));
	}

	private void updateCameraHandles()
	{
		// init handles
		if (handleCameraPos == null || handleCameraRot == null
				|| handleCameraRot == null)
		{
			setupCameraHandles();
		}

		handleCameraZoom.visible = false;
		handleCameraRot.visible = false;
		handleCameraPos.visible = false;

		if (Lttl.editor.getSettings().cameraHandles)
		{
			if (Lttl.editor.getGui().getStatusBarController().handlePosButton
					.isSelected())
			{
				handleCameraPos.visible = true;
				handleCameraPos.position.set(Lttl.game.getCamera().position);
			}
			else if (Lttl.editor.getGui().getStatusBarController().handleRotButton
					.isSelected())
			{
				handleCameraRot.visible = true;
				handleCameraRot.position.set(Lttl.game.getCamera().position);
			}
			else if (Lttl.editor.getGui().getStatusBarController().handleSclButton
					.isSelected())
			{
				handleCameraZoom.visible = true;
				handleCameraZoom.position.set(Lttl.game.getCamera().position);
			}
		}
	}

	private void setupCameraHandles()
	{
		handleCameraRot = new HandleCircle(Lttl.game.getCamera().position,
				-100, Lttl.editor.getSettings().handleSize / 2, true, true,
				false, false, false, Lttl.editor.getSettings().handleRotColor,
				null)
		{
			private float startingRotation = 0;
			private float currentRotation = 0;
			private float undoValue;

			@Override
			public void onPressed()
			{
				// save undo value
				undoValue = Lttl.game.getCamera().rotation;

				startingRotation = Lttl.game.getCamera().rotation;
				currentRotation = startingRotation;
			}

			@Override
			public void onDrag(float deltaX, float deltaY)
			{
				currentRotation -= (Lttl.input.getPixelDeltaX() / Lttl.editor
						.getSettings().handleRotationSmoothness);
				if (Lttl.editor.getInput().isShiftEV())
				{
					float leftOver = (currentRotation % 360) % 45;
					Lttl.game.getCamera().rotation = currentRotation
							+ (45 - leftOver);
				}
				else
				{
					Lttl.game.getCamera().rotation = currentRotation;
				}
			}

			@Override
			public void onReleased()
			{
				// create undo
				Lttl.editor.getUndoManager().registerUndoState(
						new UndoState("Camera - Handle Rotation",
								new UndoField(null, this.undoValue, Lttl.game
										.getCamera().rotation, new UndoSetter()
								{
									@Override
									public void set(LttlComponent comp,
											Object value)
									{
										Lttl.game.getCamera().rotation = (Float) value;
									}
								})));
			}
		};

		handleCameraZoom = new HandleCircle(Lttl.game.getCamera().position,
				-100, Lttl.editor.getSettings().handleSize / 2, true, true,
				false, false, false, Lttl.editor.getSettings().handleSclColor,
				null)
		{
			private float scaleFactor = 1;
			private float initialZoom;

			@Override
			public void onPressed()
			{
				scaleFactor = 1;
				initialZoom = Lttl.game.getCamera().zoom;
			}

			@Override
			public void onDrag(float deltaX, float deltaY)
			{
				if (Lttl.input.getEditorX() <= this.position.x)
				{
					scaleFactor += deltaX
							/ Lttl.editor.getSettings().handleScaleSmoothness
							/ 3 * Lttl.editor.getCamera().zoom;
				}
				else
				{
					scaleFactor += deltaX
							/ Lttl.editor.getSettings().handleScaleSmoothness
							* Lttl.editor.getCamera().zoom;
				}
				Lttl.game.getCamera().zoom = initialZoom * scaleFactor;
			}

			@Override
			public void onReleased()
			{
				// create undo
				Lttl.editor.getUndoManager().registerUndoState(
						new UndoState("Camera - Handle Zoom", new UndoField(
								null, initialZoom, Lttl.game.getCamera().zoom,
								new UndoSetter()
								{
									@Override
									public void set(LttlComponent comp,
											Object value)
									{
										Lttl.game.getCamera().zoom = (Float) value;
									}
								})));
			}
		};

		handleCameraPos = new HandleCircle(Lttl.game.getCamera().position,
				-100, Lttl.editor.getSettings().handleSize / 2, true, true,
				true, false, false, Lttl.editor.getSettings().handlePosColor,
				null)
		{
			private Vector2 initialPos;

			@Override
			public void onPressed()
			{
				initialPos = new Vector2(Lttl.game.getCamera().position);
			}

			@Override
			public void onDrag(float deltaX, float deltaY)
			{
				// update position of camera
				Lttl.game.getCamera().position.set(this.position);
			}

			@Override
			public void onReleased()
			{
				// create undo
				Lttl.editor.getUndoManager().registerUndoState(
						new UndoState("Camera - Handle Position",
								new UndoField(null, initialPos, new Vector2(
										Lttl.game.getCamera().position),
										new UndoSetter()
										{
											@Override
											public void set(LttlComponent comp,
													Object value)
											{
												Lttl.game.getCamera().position
														.set((Vector2) value);
											}
										})));
			}
		};
	}

	/**
	 * Sets the initial selection (on load, requires game to be fully loaded)
	 */
	void initSelection()
	{
		int[] ids = Lttl.game.getWorldCore().editorSettings.initialSelectedTransforms;
		if (ids != null && ids.length > 0)
		{
			ArrayList<LttlTransform> transforms = new ArrayList<>();
			for (int id : ids)
			{
				LttlTransform transform = (LttlTransform) Lttl.scenes
						.findComponentByIdAllScenes(id);
				if (transform == null) continue;
				transforms.add(transform);
			}
			setSelection(transforms);
		}
	}

	public boolean isSelectionLocked()
	{
		return !tree.isEnabled();
	}

	public void lockSelection()
	{
		tree.setEnabled(false);
	}

	public void unlockSelection()
	{
		tree.setEnabled(true);
	}

	/**
	 * Returns if a transform is selected.
	 * 
	 * @param transform
	 * @return
	 */
	public boolean isSelected(LttlTransform transform)
	{
		int[] ids = getSelectedTransformIds();
		return ids.length > 0
				&& LttlHelper.ArrayContains(getSelectedTransformIds(),
						transform.getId());
	}

	void pasteComponent(ArrayList<LttlTransform> onToTransforms)
	{
		if (Lttl.editor.getGui().isCut())
		{
			if (Lttl.editor.getGui().getCopyComponent().hasDependents())
			{
				GuiHelper
						.showAlert(
								null,
								"Component Required",
								"This component can't be cut because it is required by other components.",
								JOptionPane.WARNING_MESSAGE);
				return;
			}
		}

		// first check all nodes can add component
		for (LttlTransform transform : onToTransforms)
		{
			// also assume that getCopyComponent returns a non null comp
			LttlComponent comp = Lttl.editor.getGui().getCopyComponent();

			if (!transform.canAddComponentType(comp.getClass()))
			{
				JOptionPane
						.showMessageDialog(
								null,
								((Object) comp.getClass().getSimpleName()
										+ " cannot be added to transform '"
										+ transform.getName() + "'  No components were added.."),
								"Failed To Add Component",
								JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		// now actually add the component copy
		for (LttlTransform transform : onToTransforms)
		{
			// also assume that getCopyComponent returns a non null comp
			LttlComponent comp = Lttl.editor.getGui().getCopyComponent();

			transform.addComponentCopy(comp.getClass().cast(comp));

			if (Lttl.editor.getGui().getPropertiesController()
					.getFocusTransformId() == transform.getId())
			{
				Lttl.editor.getGui().getPropertiesController().draw(false);
			}
		}

		if (Lttl.editor.getGui().isCut())
		{
			Lttl.editor.getGui().getCopyComponent().destroyComp();
			Lttl.editor.getGui().setCopyComponent(null, false);
		}
	}

	private void onTransformSelectionChange(
			ArrayList<LttlTransform> selectedTransforms)
	{
		IntArray newSelectedTransforms = new IntArray(selectedTransforms.size());
		for (LttlTransform lt : selectedTransforms)
		{
			newSelectedTransforms.add(lt.getId());
		}

		// if selection changed, then update transform history
		if (!LttlHelper.ArrayItemsSame(newSelectedTransforms,
				lastSelectedTransforms, false))
		{
			lastSelectedTransforms.clear();
			lastSelectedTransforms.addAll(newSelectedTransforms);
			Lttl.editor.getGui().getStatusBarController()
					.updateTransformHistory(selectedTransforms);
		}
	}
}
