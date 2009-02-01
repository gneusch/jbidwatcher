package com.jbidwatcher.platform;
/*
 * Copyright (C) 2004 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

import org.jdesktop.jdic.tray.*;
import javax.swing.*;
import java.awt.event.*;
import java.net.URL;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import com.jbidwatcher.util.queue.MQFactory;
import com.jbidwatcher.util.queue.MessageQueue;
import com.jbidwatcher.util.config.JConfig;
import com.jbidwatcher.util.Constants;

public class Tray implements ItemListener, MessageQueue.Listener {
  private JMenuItem hideRestore;
  //  This creates a new Thread (currently named Thread 9).
  private SystemTray tray = SystemTray.getDefaultSystemTray();
  private TrayIcon ti;
  Class java6TrayClass;
  Class java6TrayIconClass;
  private Object java6tray=null, java6icon=null;

  private class TrayMenuAction implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String cmd = e.getActionCommand();

      if(cmd.startsWith("SEARCH")) {
        MQFactory.getConcrete("user").enqueue("SEARCH");
        return;
      } else if(cmd.startsWith("CONFIGURE")) {
        MQFactory.getConcrete("user").enqueue("Configure");
        return;
      }
      //  If there are no spaces, it's a command to pass straight to the Swing code.
      if(cmd.indexOf(' ') == -1) {
        MQFactory.getConcrete("Swing").enqueue(cmd);
      } else {
        String s = "Action event detected." + '\n' + "    Event source: " + e.getSource() + " (an instance of " + getClassName(e.getSource()) + ")\n    ActionCommand: " + e.getActionCommand();
        JConfig.log().logDebug(s);
      }
    }
  }

  private Tray() {
    TrayMenuAction tma = new TrayMenuAction();
    System.setProperty("javax.swing.adjustPopupLocationToFit", "false");
    JPopupMenu menu = new JPopupMenu("JBidwatcher Tray Menu");

    // a group of JMenuItems
    hideRestore = new JMenuItem("Hide");
    hideRestore.getAccessibleContext().setAccessibleDescription("Restores JBidwatcher window to the display.");
    hideRestore.addActionListener(tma);
    hideRestore.setActionCommand("HIDE");
    menu.add(hideRestore);

    menu.addSeparator();
    JMenuItem searchMenuItem = new JMenuItem("Search...");
    searchMenuItem.addActionListener(tma);
    searchMenuItem.setActionCommand("SEARCH");
    menu.add(searchMenuItem);

    JMenuItem configMenuItem = new JMenuItem("Configure...");
    configMenuItem.addActionListener(tma);
    configMenuItem.setActionCommand("CONFIGURE");
    menu.add(configMenuItem);
    menu.addSeparator();

    JMenuItem quitMenuItem = new JMenuItem("Quit", KeyEvent.VK_Q);
    quitMenuItem.setActionCommand("QUIT");
    quitMenuItem.getAccessibleContext().setAccessibleDescription("Close JBidwatcher.");
    quitMenuItem.addActionListener(tma);
    menu.add(quitMenuItem);

    // ImageIcon jbw_icon = new ImageIcon("duke.gif");
    URL iconURL = JConfig.getResource(JConfig.queryConfiguration("icon", "/jbidwatch64.jpg"));
    ImageIcon jbw_icon = new ImageIcon(iconURL);

    try {
      tryJava6Tray(menu, jbw_icon);
    } catch (ClassNotFoundException e) {
      ti = new TrayIcon(jbw_icon, "JBidwatcher", menu);
      ti.setIconAutoSize(true);

      ti.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            MQFactory.getConcrete("Swing").enqueue("VISIBILITY");
          }
      });

      ti.addBalloonActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e) {
            //  Balloon message has been clicked?
          }
      });
    }
  }

  private void tryJava6Tray(JPopupMenu menu, ImageIcon jbw_icon) throws ClassNotFoundException {
    java6TrayClass = Class.forName("java.awt.SystemTray");
    try {
      Method m = java6TrayClass.getMethod("getSystemTray");
      java6tray = m.invoke(null);
      java6TrayIconClass = Class.forName("java.awt.TrayIcon");
      Constructor<?> tiConst = java6TrayIconClass.getConstructor(ImageIcon.class, String.class, JPopupMenu.class);
      java6icon = tiConst.newInstance(jbw_icon, "JBidwatcher", menu);
      Method sIAS = java6TrayIconClass.getMethod("setIconAutoSize", Boolean.class);
      sIAS.invoke(java6icon, true);
      Method aAL = java6TrayIconClass.getMethod("addActionListener", ActionListener.class);
      aAL.invoke(java6icon, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          MQFactory.getConcrete("Swing").enqueue("VISIBILITY");
        }
      });
      Method aBAL = java6TrayIconClass.getMethod("addBalloonActionListener", ActionListener.class);
      aBAL.invoke(java6icon, new ActionListener(){
        public void actionPerformed(ActionEvent e) {
          //  Balloon message has been clicked?
        }
      });
    } catch (Exception e) {
      JConfig.log().logMessage("Failed to get system tray!");
    }
  }

  // Returns just the class name -- no package info.
  protected String getClassName(Object o) {
    String classString = o.getClass().getName();
    int dotIndex = classString.lastIndexOf(".");

    return classString.substring(dotIndex + 1);
  }

  public void itemStateChanged(ItemEvent e) {
    JMenuItem source = (JMenuItem) (e.getSource());
    StringBuffer sbuf = new StringBuffer("Item event detected.");
    sbuf.append('\n' + "    Event source: ");
    sbuf.append(source.getText());
    sbuf.append(" (an instance of ");
    sbuf.append(getClassName(source));
    sbuf.append(')');
    sbuf.append('\n');
    sbuf.append("    New state: ");
    sbuf.append((e.getStateChange() == ItemEvent.SELECTED) ? "selected" : "unselected");

    JConfig.log().logDebug(sbuf.toString());
  }

  public void messageAction(Object deQ) {
    String msg = (String)deQ;
    if(msg.startsWith("TOOLTIP ")) {
      String msgText = Constants.PROGRAM_NAME + ' ' + Constants.PROGRAM_VERS + '\n' + msg.substring(8);
      if(java6icon != null) {
        try {
          Method setTT = java6TrayIconClass.getMethod("setToolTip", String.class);
          setTT.invoke(java6icon, msgText);
        } catch (Exception e) {
          JConfig.log().logMessage("Failed to set tool tip using java6 methods!");
        }
      } else {
        ti.setToolTip(msgText);
      }
    } else if(msg.startsWith("NOTIFY ")) {
      if(java6icon != null) {
        try {
          Method display = java6TrayIconClass.getMethod("displayMessage", String.class, String.class, Integer.class);
          display.invoke(java6icon, "JBidwatcer Alert", msg.substring(7), 0);
        } catch (Exception e) {
          JConfig.log().logMessage("Failed to display notification using java6 methods!");
        }
      } else {
        ti.displayMessage("JBidwatcher Alert", msg.substring(7), TrayIcon.INFO_MESSAGE_TYPE);
      }
    } else if(msg.startsWith("HIDDEN")) {
      hideRestore.setText("Restore");
      hideRestore.setActionCommand("RESTORE");
    } else if(msg.startsWith("RESTORED")) {
      hideRestore.setText("Hide");
      hideRestore.setActionCommand("HIDE");
    } else if(msg.startsWith("TRAY")) {
      String onOff = msg.substring(5);
      if(onOff.equals("on")) {
        if(java6tray != null) {
          try {
            Method addTrayIcon = java6TrayClass.getMethod("addTrayIcon", java6TrayIconClass);
            addTrayIcon.invoke(java6tray, java6icon);
          } catch (Exception e) {
            JConfig.log().logMessage("Failed to set the tray icon using the java6 method!");
          }
        } else {
          tray.addTrayIcon(ti);
        }
      } else {
        if(java6tray != null) {
          try {
            Method removeTrayIcon = java6TrayClass.getMethod("removeTrayIcon", java6TrayIconClass);
            removeTrayIcon.invoke(java6tray, java6icon);
          } catch (Exception e) {
            JConfig.log().logMessage("Failed to remove the tray icon using the java6 method!");
          }
        } else {
          tray.removeTrayIcon(ti);
        }
      }
    }
  }

  public static void start() {
    MQFactory.getConcrete("tray").registerListener(new Tray());
  }
}
