package com.cosylab.vdct.graphics.objects;

/**
 * Copyright (c) 2002, Cosylab, Ltd., Control System Laboratory, www.cosylab.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution. 
 * Neither the name of the Cosylab, Ltd., Control System Laboratory nor the names
 * of its contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.awt.*;
import java.io.*;
import java.util.*;

import com.cosylab.vdct.Console;
import com.cosylab.vdct.Constants;
import com.cosylab.vdct.DataProvider;
import com.cosylab.vdct.Version;
import com.cosylab.vdct.graphics.*;
import com.cosylab.vdct.inspector.InspectableProperty;

import com.cosylab.vdct.vdb.*;
import com.cosylab.vdct.db.DBDataEntry;
import com.cosylab.vdct.db.DBEntry;
import com.cosylab.vdct.db.DBResolver;
import com.cosylab.vdct.db.DBTemplateEntry;
import com.cosylab.vdct.dbd.DBDConstants;
import com.cosylab.vdct.util.*;

/**
 * Insert the type's description here.
 * Creation date: (21.12.2000 20:46:35)
 * @author Matej Sekoranja
 */
public class Group
	extends ContainerObject
	implements Clipboardable, Descriptable, Flexible, Movable, SaveInterface, Selectable, SaveObject
{

	private static Group clipboard = null;
	private static String nullString = "";
	private static Group root = null;
	protected String name;
	protected String namePrefix;
	// local view settings
	ViewState localView = null;
	
	// template instances fields lookuptable 
	private Hashtable lookupTable = null;
	private static VDBTemplate editingTemplateData = null;

	// contains DB structure (entry (include, path, addpath statements), record, expand)
	protected Vector structure = null; 
	
/**
 * Group constructor comment.
 * @param parent com.cosylab.vdct.graphics.objects.ContainerObject
 */
public Group(ContainerObject parent) {
	super(parent);
	setColor(Color.black);
	setWidth(Constants.GROUP_WIDTH);
	setHeight(Constants.GROUP_HEIGHT);

	structure = new Vector();
}
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 20:46:35)
 * @param visitor com.cosylab.vdct.graphics.objects.Visitor
 */
public void accept(Visitor visitor) {
	visitor.visitGroup();
}
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 20:30:04)
 * @param id java.lang.String
 * @param object com.cosylab.vdct.graphics.objects.VisibleObject
 */
public void addSubObject(String id, VisibleObject object) {
	super.addSubObject(id, object);

	// records, group, template
	Vector structure = Group.getRoot().getStructure();
	if (object instanceof SaveObject)
	{
		structure.addElement(object);
	}
/*
	com.cosylab.vdct.undo.UndoManager.getInstance().addAction(
		new com.cosylab.vdct.undo.CreateAction(object)
	);
*/	
	if (object instanceof com.cosylab.vdct.inspector.Inspectable)
		com.cosylab.vdct.DataProvider.getInstance().fireInspectableObjectAdded((com.cosylab.vdct.inspector.Inspectable)object);
}
/**
 * Insert the method's description here.
 * IDs are relative
 * Creation date: (28.1.2001 17:10:46)
 * @param id java.lang.String
 * @param object com.cosylab.vdct.graphics.objects.VisibleObject
 * @param create boolean
 */
public void addSubObject(String id, VisibleObject object, boolean create) {
	if (id.length()==0 || id.charAt(0)==Constants.GROUP_SEPARATOR)
		com.cosylab.vdct.Console.getInstance().println("Invalid name object name '"+id+"'. Skipping...");
	else if (!Group.hasTokens(id)) addSubObject(id, object);
	else {
		Group parent = null;
		String parentName = Group.substractParentName(id);

		// find parent
		String firstParentName;
		if (Group.hasTokens(parentName))
			firstParentName = Group.substractToken(parentName);
		else
			firstParentName = parentName;
		parent = (Group)getSubObject(firstParentName);

		if (parent==null) {
			if (!create) {
				com.cosylab.vdct.Console.getInstance().println("o) Internal error: no parent found");
				com.cosylab.vdct.Console.getInstance().println("\t id="+id+", current group="+getAbsoluteName());
				return;
			}
			else {
				parent = new Group(this);
				parent.setX(object.getX());
				parent.setY(object.getY());
				parent.setName(firstParentName);
				parent.setNamePrefix(getAbsoluteName());
				addSubObject(firstParentName, parent);
			}
		}
		// first parent created, recursive call
		id = substractRelativeName(firstParentName, id);
		parent.addSubObject(id, object, create);
	}
}
/**
 * Insert the method's description here.
 * Creation date: (25.12.2000 14:14:35)
 * @return boolean
 * @param dx int
 * @param dy int
 */
public boolean checkMove(int dx, int dy) {
	ViewState view = ViewState.getInstance();

	if ((getX()<-dx) || (getY()<-dy) || 
		(getX()>(view.getWidth()-getWidth()-dx)) || (getY()>(view.getHeight()-getHeight()-dy)))
		return false;
	else
		return true;
}
/**
 * Insert the method's description here.
 * Creation date: (4.2.2001 22:02:39)
 * @param group java.lang.String
 */
public boolean copyToGroup(java.lang.String group) {
	
	String newName;
	String oldName = getAbsoluteName();
	if (group.equals(nullString))
		newName = getName();
	else
		newName = group+Constants.GROUP_SEPARATOR+getName();

	ViewState view = ViewState.getInstance();
		
	while (Group.getRoot().findObject(newName, true)!=null)
//		newName += Constants.COPY_SUFFIX;
			newName = StringUtils.incrementName(newName, Constants.COPY_SUFFIX);

	Group g = Group.createGroup(newName);
	if (group.equals(getNamePrefix()) || group.equals(Constants.CLIPBOARD_NAME)) {
		g.setX(getX()+20-view.getRx()); g.setY(getY()+20-view.getRy());
	}
	else {
		g.setX(getX()-view.getRx()); g.setY(getY()-view.getRy());
	}

	Flexible flexible;
	Object[] objs = new Object[getSubObjectsV().size()];
	getSubObjectsV().copyInto(objs);
	for (int i=0; i < objs.length; i++) {
		if (objs[i] instanceof Flexible) {
			flexible = (Flexible)objs[i];
			flexible.copyToGroup(newName);
		}
	}


	boolean monitoring = com.cosylab.vdct.undo.UndoManager.getInstance().isMonitor();
	com.cosylab.vdct.undo.UndoManager.getInstance().setMonitor(false);
	try
	{
	
		for (int i=0; i < objs.length; i++) {
			if (objs[i] instanceof Record)
				{
					Record record = (Record)g.getSubObject(Group.substractObjectName(((Record)objs[i]).getFlexibleName()));
					if (record!=null)
						record.fixEPICSOutLinks(oldName, newName);
				}
		}
	}
	catch (Exception e) {}
	finally
	{
		com.cosylab.vdct.undo.UndoManager.getInstance().setMonitor(monitoring);
	}
	
	return true;

}
/**
 * Insert the method's description here.
 * IDs are relative
 * Creation date: (28.1.2001 17:10:46)
 * @param name java.lang.String
 */
public static Group createGroup(String name) {
	Group group = new Group(null);
	group.setAbsoluteName(name);
	getRoot().addSubObject(name, group, true);
	return group;
}
/**
 * Insert the method's description here.
 * Creation date: (4.2.2001 16:23:37)
 */
public void destroy() {
	
	VisibleObject vo;
	Object[] objs = new Object[subObjectsV.size()];
	subObjectsV.copyInto(objs);
	for (int i=0; i < objs.length; i++) {
		vo = (VisibleObject)objs[i];
		if ((vo instanceof Record) ||
			(vo instanceof Group)) {
			vo.destroy();
			com.cosylab.vdct.undo.UndoManager.getInstance().addAction(
				new com.cosylab.vdct.undo.DeleteAction(vo)
			);
		}
	}
	clear();
	if (getParent()!=null) getParent().removeObject(getName());
	
}
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 20:46:35)
 * @param g java.awt.Graphics
 * @param hilited boolean
 */
protected void draw(Graphics g, boolean hilited) {

	ViewState view = ViewState.getInstance();

	int rrx = getRx()-view.getRx();
	int rry = getRy()-view.getRy();
	int rwidth = getRwidth();
	int rheight = getRheight();
		
	// clipping
	if ((!(rrx>view.getViewWidth()) || (rry>view.getViewHeight())
	    || ((rrx+rwidth)<0) || ((rry+rheight)<0))) {

		if (!hilited) g.setColor(Constants.RECORD_COLOR);
		else if (view.isPicked(this)) g.setColor(Constants.PICK_COLOR);
		else if (view.isSelected(this) ||
				 view.isBlinking(this)) g.setColor(Constants.SELECTION_COLOR);
		else g.setColor(Constants.RECORD_COLOR);
	
		g.fillRect(rrx, rry, rwidth, rheight);
		if (!hilited) g.setColor(Constants.FRAME_COLOR);
		else g.setColor((this==view.getHilitedObject()) ? 
						Constants.HILITE_COLOR : Constants.FRAME_COLOR);

		g.drawRect(rrx, rry, rwidth, rheight);

		if (getFont()!=null) {
			g.setFont(getFont());
			g.drawString(getLabel(), rrx+getRlabelX(), rry+getRlabelY());
		}
	
	}
	// paint components
/*
	VisualObject vo;				
	Enumeration e = getObjects();
	while (e.hasMoreElements()) {
	  vo = (VisualObject)(e.nextElement());
	  if (vo instanceof GroupFieldObject) vo.paint(g, dp);
	}
*/
}
/**
 * Insert the method's description here.
 * Creation date: (28.1.2001 11:13:31)
 * @return com.cosylab.vdct.graphics.objects.Record
 * @param objectName java.lang.String
 * @param deep boolean
 */
public Object findObject(String objectName, boolean deep) {
	if (objectName.length()==0 && Group.getRoot()==this)
		return this;
		
	String relName = Group.substractRelativeName(getAbsoluteName(), 
												 objectName);
	
	if (relName==null)
		return null; 	
	else if (relName.length()==0 || relName.charAt(0)==Constants.GROUP_SEPARATOR)
	{
		com.cosylab.vdct.Console.getInstance().println("Invalid name '"+objectName+"'.");
		return null; 	
	}
	else if (Group.hasTokens(relName))
	{
		if (!deep) return null;
		String parentName = Group.substractToken(relName);
		// !!! check if parent is always Group object
		Group parent = (Group)getSubObject(parentName);
		if (parent==null)
		{
			//com.cosylab.vdct.Console.getInstance().println("o) Internal error: no parent found / no such object");
			//com.cosylab.vdct.Console.getInstance().println("\t objectName="+objectName+", current group="+getAbsoluteName());
			return null;
		}
		else 
			return parent.findObject(objectName, deep);
	}
	else {
		return getSubObject(relName);
	}
}
/**
 * This method has to be called to fix links after move, copy...
 * Creation date: (30.1.2001 11:37:45)
 * @param deep boolean
 */
public void fixLinks(boolean deep) {
	
	Enumeration e = subObjectsV.elements();
	Object obj;
	while (e.hasMoreElements()) {
		obj = e.nextElement();
		if (obj instanceof Record)
			((Record)obj).fixLinks();
		else if (deep && (obj instanceof Group))
			((Group)obj).fixLinks(deep);
	}

}
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 20:52:35)
 * @return java.lang.String
 */
public String getAbsoluteName() {
	if (namePrefix.equals("")) return name; 
	else return namePrefix+Constants.GROUP_SEPARATOR+name;
}
/**
 * Insert the method's description here.
 * Creation date: (5.2.2001 14:42:24)
 * @return com.cosylab.vdct.graphics.objects.Group
 */
public static Group getClipboard() {
	if (clipboard==null) {
		clipboard = new Group(null);
		clipboard.setAbsoluteName(Constants.CLIPBOARD_NAME);
	}
	return clipboard;
}
/**
 * Insert the method's description here.
 * Creation date: (24.4.2001 17:41:21)
 * @return java.lang.String
 */
public java.lang.String getDescription() {
	return null;
}
/**
 * Insert the method's description here.
 * Creation date: (3.5.2001 10:17:13)
 * @return java.lang.String
 */
public java.lang.String getFlexibleName() {
	return getAbsoluteName();
}
/**
 * Insert the method's description here.
 * Creation date: (3.5.2001 16:42:04)
 * @return java.lang.String
 */
public java.lang.String getHashID() {
	return getName();
}
/**
 * Insert the method's description here.
 * Creation date: (3.5.2001 13:28:45)
 * @return com.cosylab.vdct.graphics.ViewState
 */
public com.cosylab.vdct.graphics.ViewState getLocalView() {
	if (localView==null) localView = new ViewState();
	return localView;
}
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 20:51:29)
 * @return java.lang.String
 */
public java.lang.String getName() {
	return name;
}
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 20:51:29)
 * @return java.lang.String
 */
public java.lang.String getNamePrefix() {
	return namePrefix;
}
/**
 * Insert the method's description here.
 * Creation date: (28.1.2001 11:32:14)
 * @return com.cosylab.vdct.graphics.objects.Group
 */
public static Group getRoot() {
	return root;
}
/**
 * Insert the method's description here.
 * Creation date: (28.1.2001 16:54:17)
 * @return java.lang.Object
 * @param id java.lang.String
 */
public Object getSubObject(String id) {
	if (id.equals(nullString))
		return getRoot();
	else if (id.equals(Constants.CLIPBOARD_NAME))		// ?!! no ignore case
		return getClipboard();
	else
		return super.getSubObject(id);
}
/**
 * Insert the method's description here.
 * Creation date: (25.4.2001 22:13:55)
 * @return int
 */
public int getX() {
	int posX = super.getX();
	if (com.cosylab.vdct.Settings.getInstance().getSnapToGrid())
		return posX - posX % Constants.GRID_SIZE;
	else
		return posX;
}
/**
 * Insert the method's description here.
 * Creation date: (25.4.2001 22:13:55)
 * @return int
 */
public int getY() {
	int posY = super.getY();
	if (com.cosylab.vdct.Settings.getInstance().getSnapToGrid())
		return posY - posY % Constants.GRID_SIZE;
	else
		return posY;
}
/**
 * Insert the method's description here.
 * Creation date: (28.1.2001 11:50:26)
 * @return boolean
 * @param name java.lang.String
 */
public static boolean hasTokens(String name) {
	return (name.indexOf(Constants.GROUP_SEPARATOR)!=-1);
}
/**
 * Returned value inicates change
 * Creation date: (21.12.2000 22:21:12)
 * @return com.cosylab.vdct.graphics.object.VisibleObject
 * @param x int
 * @param y int
 */
public VisibleObject hiliteComponentsCheck(int x, int y) {

	//ViewState view = ViewState.getInstance();
	VisibleObject spotted = null;
	
	Enumeration e = subObjectsV.elements();
	VisibleObject vo;
	while (e.hasMoreElements()) {
		vo = (VisibleObject)(e.nextElement());
		vo = vo.intersects(x, y);
		if (vo!=null) {
			// special hanadling for connectors
			if (vo instanceof Connector)
				return vo;
			/*else if (view.getHilitedObject()!=vo)
			 	spotted = vo;
			else if (spotted==null)*/
			else
			 	spotted = vo;
		}
	}

	return spotted;
}
/**
 * Insert the method's description here.
 * Creation date: (1.2.2001 14:09:39)
 */
 
public void initializeLayout() {

	ViewState view = ViewState.getInstance();
	boolean grid = com.cosylab.vdct.Settings.getInstance().getSnapToGrid();
	com.cosylab.vdct.Settings.getInstance().setSnapToGrid(false);     // avoid fixes in getX()
	try
	{
		
		// count objects to layout
		int containerCount = 0;
		Enumeration e = getSubObjectsV().elements();
		while (e.hasMoreElements())
			if (e.nextElement() instanceof ContainerObject)
				containerCount++;
					
		final int offset = 20;
		
		// groups should be the widest
		int nx = (view.getWidth()-offset)/(Constants.GROUP_WIDTH+offset);
		if (nx==0) nx=1;
		int sx = (int)((view.getWidth()-offset)/nx);
		sx = Math.min(sx, Constants.GROUP_WIDTH+offset);
			
		int ny = containerCount/nx+1;
		if (ny==0) ny=1;
		int sy = (int)(view.getHeight()/(ny+1));
		sy = Math.min(sy, 3*Constants.GROUP_HEIGHT);		

		int x = offset/2;
		int y = x;
		int i = 0;

		VisibleObject vo;
		e = getSubObjectsV().elements();
		while (e.hasMoreElements()) {
			vo = (VisibleObject)(e.nextElement());
			if (vo instanceof ContainerObject) {
				if (vo instanceof Group) ((Group)vo).initializeLayout();
				if ((vo.getX()<=0) || (vo.getY()<=0) ||
					(vo.getY()>=view.getHeight()) || 
					(vo.getX()>=view.getWidth())) {
					vo.setX(x); vo.setY(y);
					x+=sx; i++;
					if (i>=nx) {
						x=offset/2; i=0; 
						y+=sy;
					}
				}
			}
		}
	} 
	catch (Exception e)
	{
	}
	finally
	{
		com.cosylab.vdct.Settings.getInstance().setSnapToGrid(grid);
	}

}
/**
 * Insert the method's description here.
 * Creation date: (30.1.2001 11:37:45)
 * @param deep boolean
 */
public void manageLinks(boolean deep) {
	
	Enumeration e = subObjectsV.elements();
	Object obj;
	while (e.hasMoreElements()) {
		obj = e.nextElement();
		if (obj instanceof Record)
			((Record)obj).manageLinks();
		else if (obj instanceof Template)
			((Template)obj).manageLinks();
		else if (deep && (obj instanceof Group))
			((Group)obj).manageLinks(deep);
	}
}
/**
 * Insert the method's description here.
 * Creation date: (25.12.2000 14:14:35)
 * @return boolean
 * @param dx int
 * @param dy int
 */
public boolean move(int dx, int dy) {
	if (checkMove(dx, dy)) {
		setX(super.getX()+dx);
		setY(super.getY()+dy);
		revalidatePosition();
		return true;
	}
	else 
		return false;
}
/**
 * Insert the method's description here.
 * Creation date: (4.2.2001 22:02:39)
 * @param group java.lang.String
 */
public boolean moveToGroup(java.lang.String group) {
	if (group.equalsIgnoreCase(getAbsoluteName())) return false; 	// move to itself

	//String oldName = getAbsoluteName();
	String newName;
	if (group.equals(nullString))
		newName = getName();
	else
		newName = group+Constants.GROUP_SEPARATOR+getName();

	Object obj = Group.getRoot().findObject(newName, true);
	while (obj!=null && obj!=this)
	{
		//newName += Constants.MOVE_SUFFIX;
		newName = StringUtils.incrementName(newName, Constants.MOVE_SUFFIX);
		obj = Group.getRoot().findObject(newName, true);
	}
	
	//getRoot().addSubObject(newName, this, true);
	//setAbsoluteName(newName);
/*	Group g = getRoot().createGroup(newName);
	g.setX(getX()); g.setY(getY());*/
	getParent().removeObject(getName());
	setParent(null);
	setAbsoluteName(newName);
	
	Group g = (Group)getRoot().findObject(group, true);
	if (g==null) {
		g=Group.createGroup(group);
	}
	if (g==null) return false;
/*	if (((Group)getParent()).localView!=null)
	{
		g.setX((int)(view.getRx()/view.getScale()));
		g.setX((int)(view.getRy()/view.getScale()));
	}*/
	g.addSubObject(getName(), this);
	
	Flexible flexible;
	Object[] objs = new Object[subObjectsV.size()];
	getSubObjectsV().copyInto(objs);
	for (int i=0; i < objs.length; i++) {
		if (objs[i] instanceof Flexible) {
			flexible = (Flexible)objs[i];
			flexible.moveToGroup(newName);
		}
	}

	return true;
/*
	// remove if empty
	if (getSubObjectsV().size()==0)
		if (getParent()!=null) getParent().removeObject(getName());*/
}
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 21:58:56)
 * @param g java.awt.Graphics
 * @param hilited boolean
 */
public void paintComponents(Graphics g, boolean hilited) {
	paintComponents(g, hilited, false);
}
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 21:58:56)
 * @param g java.awt.Graphics
 * @param hilited boolean
 */
public void paintComponents(Graphics g, boolean hilited, boolean flatten) {
	Enumeration e = subObjectsV.elements();

	if (flatten)
	{
		Object obj;
		while (e.hasMoreElements()) {
			obj = e.nextElement();
			if (obj instanceof Group)
				((Group)obj).paintComponents(g, hilited, true);
			else
				((VisibleObject)obj).paint(g, hilited);
		}
		// no post paint here!!!
	}
	else
	{
		VisibleObject vo;
		while (e.hasMoreElements()) {
			vo = (VisibleObject)(e.nextElement());
			vo.paint(g, hilited);
		}
		
		e = subObjectsV.elements();
		while (e.hasMoreElements()) {
			vo = (VisibleObject)(e.nextElement());
			vo.postPaint(g, hilited);
		}
	}
	
}

/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 20:32:49)
 * @param id java.lang.String
 * @return java.lang.Object
 */
public Object removeObject(String id) {
	Object object = super.removeObject(id);
/*
	com.cosylab.vdct.undo.UndoManager.getInstance().addAction(
		new com.cosylab.vdct.undo.DeleteAction((VisibleObject)object)
	);
*/	
	Vector structure = Group.getRoot().getStructure();
	if (object instanceof SaveObject)
	{
		structure.remove(object);
	}
	
	if (object instanceof com.cosylab.vdct.inspector.Inspectable)
		com.cosylab.vdct.DataProvider.getInstance().fireInspectableObjectRemoved((com.cosylab.vdct.inspector.Inspectable)object);
	return object;
}
/**
 * Insert the method's description here.
 * Creation date: (2.5.2001 23:23:42)
 * @param newName java.lang.String
 */
public boolean rename(java.lang.String newName) {

	//String oldName = getAbsoluteName();
	String newObjName = Group.substractObjectName(newName);

	getParent().removeObject(getName());
	setName(newObjName);
	getParent().addSubObject(getName(), this);

	// move if needed
	if (!moveToGroup(Group.substractParentName(newName)))
	{
		Flexible flexible;
		Object[] objs = new Object[subObjectsV.size()];
		getSubObjectsV().copyInto(objs);
		for (int i=0; i < objs.length; i++) {
			if (objs[i] instanceof Flexible) {
				flexible = (Flexible)objs[i];
				flexible.moveToGroup(newName);
			}
		}
	}

	return true;
	
}
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 21:22:45)
 */
public void revalidatePosition() {
  setRx((int)(getX()*getRscale()));
  setRy((int)(getY()*getRscale()));
}
/**
 * Insert the method's description here.
 * Creation date: (27.12.2000 12:45:23)
 * @return boolean
 */
public boolean selectAllComponents() {
	
	ViewState view = ViewState.getInstance();
	boolean anyNew = false;
	
	Enumeration e = subObjectsV.elements();
	VisibleObject vo;
	while (e.hasMoreElements()) {
		vo = (VisibleObject)(e.nextElement());
		if (vo instanceof Selectable)
			if (view.setAsSelected(vo)) anyNew = true;
	}

	return anyNew;
}

/**
 * Returned value inicates change
 * Creation date: (21.12.2000 22:21:12)
 * @return boolean anyNew
 * @param x1 int
 * @param y1 int
 * @param x2 int
 * @param y2 int
 */
public boolean selectComponentsCheck(int x1, int y1, int x2, int y2) {

	int t;
	if (x1>x2)
		{ t=x1; x1=x2; x2=t; }
	if (y1>y2)
		{ t=y1; y1=y2; y2=t; }

	ViewState view = ViewState.getInstance();
	boolean anyNew = false;
	
	Enumeration e = subObjectsV.elements();
	VisibleObject vo;
	while (e.hasMoreElements()) {
		vo = (VisibleObject)(e.nextElement());
		if ((vo instanceof Selectable) && 
			 (vo.intersects(x1, y1, x2, y2)!=null)) {
				if (view.setAsSelected(vo)) anyNew = true;
		}

		if (vo instanceof SelectableComponents) {
			if (((SelectableComponents)vo).selectComponentsCheck(x1, y1, x2, y2)) anyNew = true;
		}
	}

	return anyNew;
}

/**
 * Insert the method's description here.
 * Creation date: (28.1.2001 11:43:27)
 * @param absoluteName java.lang.String
 */
public void setAbsoluteName(String absoluteName) {
	int lastSepPos = absoluteName.lastIndexOf(Constants.GROUP_SEPARATOR);
	if (lastSepPos==-1) {
		name = absoluteName; namePrefix = nullString;
	}
	else {
		name = absoluteName.substring(lastSepPos+1);
		namePrefix = absoluteName.substring(0, lastSepPos);
	}
}
/**
 * Insert the method's description here.
 * Creation date: (24.4.2001 17:41:21)
 * @param description java.lang.String
 */
public void setDescription(java.lang.String description) {}
/**
 * Insert the method's description here.
 * Creation date: (3.5.2001 13:28:45)
 * @param newLocalView com.cosylab.vdct.graphics.ViewState
 */
public void setLocalView(com.cosylab.vdct.graphics.ViewState newLocalView) {
	localView = newLocalView;
}
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 20:51:29)
 * @param newName java.lang.String
 */
public void setName(java.lang.String newName) {
	name = newName;
}
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 20:51:29)
 * @param newNamePrefix java.lang.String
 */
public void setNamePrefix(java.lang.String newNamePrefix) {
	namePrefix = newNamePrefix;
}
/**
 * Insert the method's description here.
 * Creation date: (28.1.2001 11:32:14)
 * @param newRoot com.cosylab.vdct.graphics.objects.Group
 */
public static void setRoot(Group newRoot) {
	root = newRoot;
}
/**
 * Insert the method's description here.
 * Creation date: (28.1.2001 18:15:47)
 * @return java.lang.String
 * @param name java.lang.String
 */
public static String substractObjectName(String name) {
	int lastSepPos = name.lastIndexOf(Constants.GROUP_SEPARATOR);
	if (lastSepPos==-1) return name;
	else return name.substring(lastSepPos+1);
}
/**
 * Insert the method's description here.
 * Creation date: (28.1.2001 18:15:47)
 * @return java.lang.String
 * @param name java.lang.String
 */
public static String substractParentName(String name) {
	int lastSepPos = name.lastIndexOf(Constants.GROUP_SEPARATOR);
	if (lastSepPos==-1) return nullString;
	else return name.substring(0, lastSepPos);
}
/**
 * Insert the method's description here.
 * Creation date: (28.1.2001 11:36:31)
 */
public static String substractRelativeName(String groupName, String objName) {
	if (!objName.startsWith(groupName)) return null;
	else if (groupName.equals(nullString)) return objName;
	else return objName.substring(groupName.length()+1);
}
/**
 * Insert the method's description here.
 * Creation date: (28.1.2001 11:49:56)
 * @return java.lang.String
 * @param name java.lang.String
 */
public static String substractToken(String name) {
	int lastSepPos = name.indexOf(Constants.GROUP_SEPARATOR);
	if (lastSepPos==-1) return nullString;
	else return name.substring(0, lastSepPos);
}
/**
 * Insert the method's description here.
 * Creation date: (26.1.2001 17:19:47)
 */
public void unconditionalValidateSubObjects(boolean flat) {

	Enumeration e = subObjectsV.elements();
	Object obj;
	while (e.hasMoreElements()) {
		obj = e.nextElement();
		if (flat && obj instanceof Group)
		{
			((VisibleObject)obj).unconditionalValidation();
			((Group)obj).unconditionalValidateSubObjects(true);
		}
		else
			((VisibleObject)obj).unconditionalValidation();
	}
	
}
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 20:46:35)
 */
protected void validate() {
  revalidatePosition();
	
  double scale = getRscale();
  int rwidth = (int)(getWidth()*scale);
  int rheight = (int)(getHeight()*scale);
  setRwidth(rwidth);
  setRheight(rheight);

  // set appropriate font size
  int x0 = (int)(16*scale);		// insets
  int y0 = (int)(8*scale);

  setLabel(getName());

  Font font;
  font = FontMetricsBuffer.getInstance().getAppropriateFont(
	  			Constants.DEFAULT_FONT, Font.PLAIN, 
	  			getLabel(), rwidth-x0, rheight-y0);

  if (rwidth<(2*x0)) font = null;
  else
  if (font!=null) {
	  FontMetrics fm = FontMetricsBuffer.getInstance().getFontMetrics(font);
	  setRlabelX((rwidth-fm.stringWidth(getLabel()))/2);
 	  setRlabelY((rheight-fm.getHeight())/2+fm.getAscent());
  }
  setFont(font);

 
}
/**
 * Insert the method's description here.
 * Creation date: (26.1.2001 17:19:47)
 */
public void validateSubObjects() {

	Enumeration e = subObjectsV.elements();
	Object obj;
	while (e.hasMoreElements()) {
		obj = e.nextElement();
		((VisibleObject)obj).forceValidation();
	}
	
}
/**
 * Insert the method's description here.
 * Creation date: (22.4.2001 21:51:25)
 * @param file java.io.DataOutputStream
 * @param path2remove java.lang.String
 * @exception java.io.IOException The exception description.
 */
public void writeObjects(java.io.DataOutputStream file, NameManipulator namer, boolean export) throws java.io.IOException {
	//writeObjects(getSubObjectsV(), file, namer, export);
	writeObjects(Group.getRoot().getStructure(), file, namer, export);
}

/**
 * Insert the method's description here.
 * Creation date: (22.4.2001 21:51:25)
 * @param file java.io.DataOutputStream
 * @exception java.io.IOException The exception description.
 */
public static void writeObjects(Vector elements, java.io.DataOutputStream file, NameManipulator namer, boolean export) throws java.io.IOException {

 Object obj;
 String name;
 Template template;
 Record record;
 VDBFieldData fieldData = null;
 VDBRecordData recordData;
 Enumeration e2;

 int pos;
 VDBFieldData dtypFieldData;
 
 final String comma = ", ";
 final String nl = "\n";
 final String recordStart = nl+DBResolver.RECORD+"("; 
 final String fieldStart = "  "+DBResolver.FIELD+"("; 
 
 Enumeration e = elements.elements();
 while (e.hasMoreElements()) 
 	{
	 	obj = e.nextElement();
	
	 	// go through the records	
	 	if (obj instanceof Record)
	 		{
			 	record = (Record)obj;
	 			recordData = record.getRecordData();

			 	name = StringUtils.quoteIfMacro(namer.getResolvedName(recordData.getName()));

			 	// write comment
			 	if (recordData.getComment()!=null)
			 		file.writeBytes(nl+recordData.getComment());
			 		
				// write "record" block
		 		file.writeBytes(recordStart+recordData.getType()+comma+name+") {"+nl);


		 		// locate DTYP field
 				dtypFieldData = recordData.getField("DTYP");
 				if (dtypFieldData!=null)
 					{

			 		// check if DTYP field is before DBF_INLINK/DBF_OUTLINK fields
	 				pos = 0;
			 		e2 = recordData.getFieldsV().elements();
					while (e2.hasMoreElements() && fieldData!=dtypFieldData)
						{
							fieldData = (VDBFieldData)(e2.nextElement());
							if ((fieldData.getType()==DBDConstants.DBF_INLINK) ||
							    (fieldData.getType()==DBDConstants.DBF_OUTLINK)) 
								break;
							pos++;
						}
			
			 		if (fieldData!=dtypFieldData)
			 			{
					 		// move DTYP before first occurence of DBF_INLINK/DBF_OUTLINK fields
					 		recordData.getFieldsV().removeElement(fieldData);
	 						recordData.getFieldsV().insertElementAt(fieldData, pos);
				 		}
 					}

				// write fields	 			
		 		e2 = recordData.getFieldsV().elements();
				while (e2.hasMoreElements())
					{
						fieldData = (VDBFieldData)(e2.nextElement());

						// write comment
						if (fieldData.getComment()!=null)
							file.writeBytes(fieldData.getComment()+nl);
							
						String value = StringUtils.removeQuotes(fieldData.getValue());
						
						if (export)
						{
							// apply ports
							if (namer.getPorts()!=null && value!=null)
								 value = VDBTemplateInstance.applyPorts(value, namer.getPorts());
	
							// apply macro substitutions
							if (namer.getSubstitutions()!=null && value!=null)
								 value = VDBTemplateInstance.applyProperties(value, namer.getSubstitutions());
						}
												
						// if value is different from init value
						if (!fieldData.hasDefaultValue())
						{
			    				// write field value
								if (((fieldData.getType()==DBDConstants.DBF_INLINK) ||
								    (fieldData.getType()==DBDConstants.DBF_OUTLINK) ||
								    (fieldData.getType()==DBDConstants.DBF_FWDLINK))
								    && !value.startsWith(Constants.HARDWARE_LINK))
								  value = namer.getResolvedName(value);
								
								file.writeBytes(fieldStart+fieldData.getName()+comma+"\""+value+"\")"+nl);
				    
		 				}
						// write field value if has a comment
		 				else if (fieldData.getComment()!=null)
	 			       	{

							if (value.equals(nullString))
								// comment it out if it has null value
				 				file.writeBytes("  "+com.cosylab.vdct.db.DBConstants.commentString+
					 							DBResolver.FIELD+"("+fieldData.getName()+comma+"\""+value+"\")"+nl);
							else
							{
			    				// write field value
								if (((fieldData.getType()==DBDConstants.DBF_INLINK) ||
								    (fieldData.getType()==DBDConstants.DBF_OUTLINK) ||
								    (fieldData.getType()==DBDConstants.DBF_FWDLINK))
								    && !value.startsWith(Constants.HARDWARE_LINK))
								  value = namer.getResolvedName(value);

								file.writeBytes(fieldStart+fieldData.getName()+comma+"\""+value+"\")"+nl);
							}								
			 			}
	 				}
	
					file.writeBytes("}"+nl);
 			 }
		/*else if (obj instanceof Group)
 	 		{
			 	 group = (Group)obj;
			 	 group.writeObjects(file, namer, export);
	 		}*/
		else if (obj instanceof Template)
 	 		{
			 	 template = (Template)obj;
			 	 template.writeObjects(file, namer, export);
	 		}
		else if (!export && obj instanceof DBTemplateEntry)
			{
				writeTemplateData(file, namer);
				// !!! TBD multiple templates support
			}	 		
		else if (obj instanceof DBDataEntry)
 	 		{
			 	 DBDataEntry entry = (DBDataEntry)obj;
			 	
			 	// write comment
			 	if (entry.getComment()!=null)
			 		file.writeBytes(entry.getComment()+nl);
			 	
			 	//write data
			 	file.writeBytes(entry.getData()+nl);
	 		}
  }

}



/**
 * Insert the method's description here.
 */
private static void writeIncludes(java.io.DataOutputStream file) throws IOException
{

}

/**
 * Insert the method's description here.
 * Creation date: (22.4.2001 21:51:25)
 * @param file java.io.DataOutputStream
 * @param path2remove java.lang.String
 * @exception java.io.IOException The exception description.
 */
public void writeVDCTData(java.io.DataOutputStream file, NameManipulator namer, boolean export) throws java.io.IOException {
	writeVDCTData(getSubObjectsV(), file, namer, export);
}

/**
 * Insert the method's description here.
 * Creation date: (22.4.2001 21:51:25)
 * @param file java.io.DataOutputStream
 * @param path2remove java.lang.String
 * @exception java.io.IOException The exception description.
 */
public static void writeVDCTData(Vector elements, java.io.DataOutputStream file, NameManipulator namer, boolean export) throws java.io.IOException {

 Object obj;
 Group group;
 Record record;
 Template template;
 Connector connector;
 EPICSLinkOut link; 
 EPICSLink field; 
 Enumeration e2;
 
 final String nl = "\n";

 final String comma = ",";
 final String quote = "\"";
 final String ending = ")"+nl;

 final String RECORD_START    = "#! "+DBResolver.VDCTRECORD+"(";
 final String GROUP_START     = "#! "+DBResolver.VDCTGROUP+"(";
 final String FIELD_START     = "#! "+DBResolver.VDCTFIELD+"(";
 final String VISIBILITY_START= "#! "+DBResolver.VDCTVISIBILITY+"(";
 final String LINK_START      = "#! "+DBResolver.VDCTLINK+"(";
 final String CONNECTOR_START = "#! "+DBResolver.VDCTCONNECTOR+"(";

 final String LINE_START = "#! "+DBResolver.VDCTLINE+"(";
 final String BOX_START = "#! "+DBResolver.VDCTBOX+"(";
 final String TEXTBOX_START = "#! "+DBResolver.VDCTTEXTBOX+"(";

 final String TEMPLATE_INSTANCE_START  = "#! "+DBResolver.TEMPLATE_INSTANCE+"(";
 final String TEMPLATE_FIELD_START  = "#! "+DBResolver.TEMPLATE_FIELD+"(";

 final String NULL  = "null";
 	
 Enumeration e = elements.elements();
 while (e.hasMoreElements()) 
 	{
	 	obj = e.nextElement();
	
	 	if (obj instanceof Record)
	 		{
			 	record = (Record)obj;

	 			file.writeBytes(RECORD_START+
			 		StringUtils.quoteIfMacro(
				 		namer.getResolvedName(record.getRecordData().getName())
				 	) + comma + record.getX() + comma + record.getY() + 
				 	comma + StringUtils.color2string(record.getColor()) +
				 	comma + StringUtils.boolean2str(record.isRight()) +
					comma + quote + 
					namer.getResolvedName(record.getDescription()) + 
					quote +	ending);

	 			e2 = record.getSubObjectsV().elements();
	 			while (e2.hasMoreElements())
	 			{
			 		obj = e2.nextElement();
				 	if (obj instanceof Connector)
			 		{
			 			connector = (Connector)obj;

						String target = NULL;
						if (connector.getInput()!=null)
							target = namer.getResolvedName(connector.getInput().getID());
							
			 			file.writeBytes(CONNECTOR_START+
					 		StringUtils.quoteIfMacro(
						 		namer.getResolvedName(connector.getID())
						 	) + comma +  
					 		StringUtils.quoteIfMacro(
						 		target
						 	) + comma + connector.getX() + comma + connector.getY() + 
						 	comma + StringUtils.color2string(connector.getColor()) +
							comma + quote + /*!!!+ StringUtils.removeBegining(connector.getDescription(), path2remove) +*/ quote +
							comma + connector.getMode() +
							ending);
		 			 }
			 		else
			 		{
				 		if (obj instanceof EPICSLink)
				 		{
				 			field = (EPICSLink)obj;
	
				 			file.writeBytes(FIELD_START+
						 		StringUtils.quoteIfMacro(
							 		namer.getResolvedName(field.getFieldData().getFullName())
							 	) + 
							 	comma + StringUtils.color2string(field.getColor()) +
							 	comma + StringUtils.boolean2str(field.isRight()) +
								comma + quote + namer.getResolvedName(field.getDescription()) + quote + 
								ending);

			 			 }
				 		if (obj instanceof EPICSLinkOut)
				 		{
				 			link = (EPICSLinkOut)obj;
				 			if (link.getInput()!=null)
				 				file.writeBytes(LINK_START+
						 			StringUtils.quoteIfMacro(
							 			namer.getResolvedName(link.getFieldData().getFullName())
							 		) + comma + 
					 				StringUtils.quoteIfMacro(
						 				namer.getResolvedName(link.getInput().getID())
							 		) +
									ending);
			 			 }
			 		}
	 			}

				VDBFieldData vfd;
	 			e2 = record.getRecordData().getFieldsV().elements();
	 			while (e2.hasMoreElements())
	 			{
			 		vfd = (VDBFieldData)e2.nextElement();
					if (vfd.getVisibility()!=InspectableProperty.NON_DEFAULT_VISIBLE)
		 				file.writeBytes(VISIBILITY_START+
				 			StringUtils.quoteIfMacro(
					 			namer.getResolvedName(vfd.getFullName())
					 		) + comma +
					 		String.valueOf(vfd.getVisibility()) +
							ending);
	 			}


 			 }
 	 	else if (obj instanceof Group)
 	 		{
			 	 group = (Group)obj;
			 	 file.writeBytes(GROUP_START+
 					StringUtils.quoteIfMacro(
	 					namer.getResolvedName(group.getAbsoluteName())
		 			 ) + comma + group.getX() + comma + group.getY() + 
				 	 comma + StringUtils.color2string(group.getColor()) +
					 comma + quote /*+ connector.getDescription() */+ quote +
					 ending);
			 	 group.writeVDCTData(file, namer, export);
	 		}
 	 	else if (obj instanceof Line)
 	 		{
			 	 Line line = (Line)obj;
			 	 file.writeBytes(LINE_START+
 					StringUtils.quoteIfMacro(
	 					namer.getResolvedName(line.getName())
		 			 ) +
		 			 comma + line.getStartVertex().getX() + comma + line.getStartVertex().getY() +
		 			 comma + line.getEndVertex().getX() + comma + line.getEndVertex().getY() + 
				 	 comma + StringUtils.boolean2str(line.getDashed()) +
				 	 comma + StringUtils.boolean2str(line.getStartArrow()) +
				 	 comma + StringUtils.boolean2str(line.getEndArrow()) +
				 	 comma + StringUtils.color2string(line.getColor()) +
					 ending);
	 		}
 	 	else if (obj instanceof Box)
 	 		{
			 	 Box box = (Box)obj;
			 	 file.writeBytes(BOX_START+
 					StringUtils.quoteIfMacro(
	 					namer.getResolvedName(box.getName())
		 			 ) +
		 			 comma + box.getStartVertex().getX() + comma + box.getStartVertex().getY() +
		 			 comma + box.getEndVertex().getX() + comma + box.getEndVertex().getY() + 
				 	 comma + StringUtils.boolean2str(box.getIsDashed()) +
				 	 comma + StringUtils.color2string(box.getColor()) +
					 ending);
	 		}

 	 	else if (obj instanceof TextBox)
 	 		{
			 	 TextBox box = (TextBox)obj;
			 	 file.writeBytes(TEXTBOX_START+
 					StringUtils.quoteIfMacro(
	 					namer.getResolvedName(box.getName())
		 			 ) +
		 			 comma + box.getStartVertex().getX() + comma + box.getStartVertex().getY() +
		 			 comma + box.getEndVertex().getX() + comma + box.getEndVertex().getY() + 
				 	 comma + StringUtils.boolean2str(box.isBorder()) +
					 comma + quote + box.getFont().getFamily() + quote +	
					 comma + box.getFont().getSize() +	
					 comma + box.getFont().getStyle() +	
				 	 comma + StringUtils.color2string(box.getColor()) +
					 comma + quote + StringUtils.removeQuotesAndLineBreaks(box.getDescription()) + quote +
					 ending);
	 		}

 	 	else if (obj instanceof Template)
 	 		{
			 	 template = (Template)obj;
				 String templateName = namer.getResolvedName(template.getName());

			     file.writeBytes(nl);
			 	 file.writeBytes(TEMPLATE_INSTANCE_START+
 					 quote + templateName + quote +
		 			 comma + template.getX() + comma + template.getY() + 
				 	 comma + StringUtils.color2string(template.getColor()) +
					 comma + quote /*+ template.getDescription()*/ + quote +
					 ending);

				// write fields (preserve order)
	 			e2 = template.getSubObjectsV().elements();
	 			while (e2.hasMoreElements())
	 			{
						
			 		obj = e2.nextElement();
				 	if (obj instanceof EPICSLink)
			 		{
			 			field = (EPICSLink)obj;

		 				String name = field.getFieldData().getName();
						
					 	file.writeBytes(TEMPLATE_FIELD_START+
		 					 quote + templateName + quote +
		 					 comma + quote + name + quote +
							 comma + StringUtils.color2string(field.getColor()) +
							 comma + StringUtils.boolean2str(field.isRight()) +
							 comma + field.getFieldData().getVisibility() +
							 ending);
		 			 }
				 }					 

			     file.writeBytes(nl);
	 		}

 	 		
 	 }


}


/**
 * Insert the method's description here.
 */
private static void writeUsedDBDs(File dbFile, DataOutputStream stream) throws IOException
{
	 // write used DBDs
	 //String dbFilePath = dbFile.getAbsolutePath().replace('\\', '/');
	 stream.writeBytes(DBResolver.DBD_START);
	 Enumeration edbd = DataProvider.getInstance().getDBDs().elements();
	 while (edbd.hasMoreElements())
	 {
	 		File dbdFile = (File)edbd.nextElement(); 
			try
			{
				// make path relative to <dbFile> path
				dbdFile = PathSpecification.getRelativeName(dbdFile, dbFile);
			}
			catch (Exception ex)
			{
				// in case of any exception
				// do not bail-out, use given dbdFile path
				// and print stack trace
				ex.printStackTrace();
			}

	 		String file = dbdFile.toString();
	 		file = file.replace('\\', '/');

	 		// replace back-slash separator
			stream.writeBytes(DBResolver.DBD_ENTRY+file+"\")\n");
	 }
	 stream.writeBytes(DBResolver.DBD_END);
	 stream.writeBytes("\n");
}

/**
 * Insert the method's description here.
 */
private static void writeTemplateData(DataOutputStream stream, NameManipulator namer) throws IOException
{
	final String nl = "\n";
	
	final String comma = ", ";
	final String quote = "\"";
	final String ending = ")"+nl;

	VDBTemplate data = Group.getEditingTemplateData();
	
	// does not have template data (new template case)
	if (data==null)
		return; 
	
 	// write comment (even if not template definition is needed)
 	if (data.getComment()!=null)
 		stream.writeBytes(nl+data.getComment());

	// template block not needed
	if ((data.getRealDescription()==null || data.getRealDescription().length()==0) && 
		  data.getPorts().isEmpty())
		 //data.getInputs().isEmpty() && data.getOutputs().isEmpty())
		return;
	
	// template start
	stream.writeBytes(nl+DBResolver.TEMPLATE+"(");
	
	if (data.getRealDescription()!=null && data.getRealDescription().length()>0)
		stream.writeBytes(quote + StringUtils.removeQuotes(data.getRealDescription()) + quote);
	
	stream.writeBytes(") {"+nl);

	final String portStart = "  "+DBResolver.PORT+"(";

	Iterator i = data.getPortsV().iterator();
	while (i.hasNext())
	{
		VDBPort port = (VDBPort)i.next();
	
		if (port.getComment()!=null)
			stream.writeBytes(port.getComment()+nl);

		stream.writeBytes(portStart+
			port.getName() +
			comma + quote + StringUtils.removeQuotes(port.getTarget()) + quote);
		if (port.getRealDescription()!=null && port.getRealDescription().length()>0)
			stream.writeBytes(comma + quote + StringUtils.removeQuotes(port.getRealDescription()) + quote);
		stream.writeBytes(ending);
	}	
	
	boolean first = true;

	final String portPrefix = "  #! ";
	final String portPostfix = "(";
	final String macroPrefix = "  #! ";
	final String macroPostfix = "(";
	final String NULL = "null";
	final String justComma = ",";
	
	//
	// write port visual data
	//
	i = data.getPortsV().iterator();
	while (i.hasNext())
	{
		VDBPort port = (VDBPort)i.next();
	
		Port visiblePort = port.getVisibleObject();
		if (visiblePort==null)
			continue;
			
		// separate visual data
		if (first)
		{
			stream.writeBytes(nl); first = false;
		}

		switch (visiblePort.getMode())
		{
			case OutLink.CONSTANT_PORT_MODE:
				stream.writeBytes(portPrefix);
				stream.writeBytes(DBResolver.VDCT_CONSTANT_PORT);
				break;
			case OutLink.INPUT_PORT_MODE:
				stream.writeBytes(portPrefix);
				stream.writeBytes(DBResolver.VDCT_INPUT_PORT);
				break;
			case OutLink.OUTPUT_PORT_MODE:
				stream.writeBytes(portPrefix);
				stream.writeBytes(DBResolver.VDCT_OUTPUT_PORT);
				break;
			default:
				// this should never happen
				Console.getInstance().println("Warning: unknown port type for port '"+port.getName()+"'. Skipping visual definition...");
				continue;
		}

		stream.writeBytes(portPostfix);
		
		String target = NULL;
		if (visiblePort.getInput()!=null)
			target = namer.getResolvedName(visiblePort.getInput().getID());
		
		stream.writeBytes(
			visiblePort.getName() +
			justComma + StringUtils.quoteIfMacro(target) +
			justComma + visiblePort.getX() + justComma + visiblePort.getY() + 
			justComma + StringUtils.color2string(visiblePort.getColor()) +
			justComma + port.getVisibility());
		stream.writeBytes(ending);
	}	

	//
	// write macro visual data
	//
	i = data.getMacrosV().iterator();
	while (i.hasNext())
	{
		VDBMacro macro = (VDBMacro)i.next();
	
		Macro visibleMacro = macro.getVisibleObject();
		if (visibleMacro==null)
			continue;
			
		// separate visual data
		if (first)
		{
			stream.writeBytes(nl); first = false;
		}

		switch (visibleMacro.getMode())
		{
			case InLink.INPUT_MACRO_MODE:
				stream.writeBytes(macroPrefix);
				stream.writeBytes(DBResolver.VDCT_INPUT_MACRO);
				break;
			case InLink.OUTPUT_MACRO_MODE:
				stream.writeBytes(macroPrefix);
				stream.writeBytes(DBResolver.VDCT_OUTPUT_MACRO);
				break;
			default:
				// this should never happen
				Console.getInstance().println("Warning: unknown macro type for macro '"+macro.getName()+"'. Skipping visual definition...");
				continue;
		}

		stream.writeBytes(macroPostfix);
		
		stream.writeBytes(
			visibleMacro.getName() +
			justComma + quote + StringUtils.removeQuotes(macro.getDescription()) + quote +
			justComma + visibleMacro.getX() + justComma + visibleMacro.getY() + 
			justComma + StringUtils.color2string(visibleMacro.getColor()) +
			justComma + macro.getVisibility());
		stream.writeBytes(ending);
	}	

	// template end
	stream.writeBytes("}"+nl);


/*	
	// inputs
	Enumeration e = data.getInputs().keys();
	while (e.hasMoreElements())
	{
		String key = e.nextElement().toString();
		VDBFieldData fdata = (VDBFieldData)data.getInputs().get(key);
		stream.writeBytes("#! "+DBResolver.TEMPLATE_INPUT+"("+
			quote + key + quote +
			comma + quote + fdata.getFullName() + quote +
			comma + quote + data.getInputComments().get(key).toString() + quote + ending);
	}
	
	if (!data.getInputs().isEmpty()) stream.writeBytes(nl);	
	
	// outputs
	e = data.getOutputs().keys();
	while (e.hasMoreElements())
	{
		String key = e.nextElement().toString();
		VDBFieldData fdata = (VDBFieldData)data.getOutputs().get(key);
		stream.writeBytes("#! "+DBResolver.TEMPLATE_OUTPUT+"("+
			quote + key + quote +
			comma + quote + fdata.getFullName() + quote +
			comma + quote + data.getOutputComments().get(key).toString() + quote + ending);
	}

	if (!data.getOutputs().isEmpty()) stream.writeBytes(nl);	
*/
}

/**
 * Insert the method's description here.
 */
public static void save(Group group2save, File file, boolean export) throws IOException
{
	// create new namer	 
	String path2remove = group2save.getAbsoluteName();
	if (!path2remove.equals(nullString)) path2remove+=Constants.GROUP_SEPARATOR;
	else path2remove = null;
	
	NameManipulator namer = new DefaultNamer(file, path2remove, null, null, Template.preparePorts(group2save, null));
	save(group2save, file, namer, export);
}

/**
 * Insert the method's description here.
 */
public static void save(Group group2save, File file, NameManipulator namer, boolean export) throws IOException
{
	DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
	
	stream.writeBytes("#! Generated by VisualDCT v"+Version.VERSION+"\n");
	
	if (!export)	
	{
		writeUsedDBDs(file, stream);
	}
	
	writeIncludes(stream);
	
	// if not already present
	// optimize !!!
	boolean found = false;
	Iterator i = Group.getRoot().getStructure().iterator();
	while (i.hasNext() && !found)
		if (i.next() instanceof DBTemplateEntry)
			found = true;
	
	if (!found)
	{
		// put it after all DBEntry objects
		int pos = 0;
		i = Group.getRoot().getStructure().iterator();
		while (i.hasNext() && (i.next() instanceof DBEntry))
			pos++;
		Group.getRoot().getStructure().insertElementAt(new DBTemplateEntry(), pos);
	}

	group2save.writeObjects(stream, namer, export);

	if (!export)	
	{
		stream.writeBytes("\n#! Further lines contain data used by VisualDCT\n");
		group2save.writeVDCTData(stream, namer, export);
	}
		
	stream.flush();
	stream.close();
}

/**
 * Returns the lookupTable.
 * @return Hashtable
 */
public Hashtable getLookupTable()
{
	return lookupTable;
}


/**
 * Sets the lookupTable.
 * @param lookupTable The lookupTable to set
 */
public void setLookupTable(Hashtable lookupTable)
{
	this.lookupTable = lookupTable;
}

/**
 * Returns the editingTemplateData.
 * @return VDBTemplate
 */
public static VDBTemplate getEditingTemplateData()
{
	return editingTemplateData;
}

/**
 * Sets the editingTemplateData.
 * @param editingTemplateData The editingTemplateData to set
 */
public static void setEditingTemplateData(VDBTemplate editingTemplateData)
{
	Group.editingTemplateData = editingTemplateData;
}

	/**
	 * Returns the structure.
	 * @return Vector
	 */
	public Vector getStructure()
	{
		return structure;
	}

/**
 * @param linkableMacros
 * @param macros
 * @param deep
 */
public void generateMacros(HashMap macros, boolean deep) {

	Enumeration e = subObjectsV.elements();
	Object obj;
	while (e.hasMoreElements()) {
		obj = e.nextElement();
		if (obj instanceof Record)
			((Record)obj).generateMacros(macros);
		if (obj instanceof Template)
			((Template)obj).generateMacros(macros);
		else if (deep && (obj instanceof Group))
			((Group)obj).generateMacros(macros, deep);
	}
}

}
