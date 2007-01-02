package com.jbidwatcher.ui;
/*
 * Copyright (c) 2000-2005 CyberFOX Software, Inc. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the
 *  Free Software Foundation, Inc.
 *  59 Temple Place
 *  Suite 330
 *  Boston, MA 02111-1307
 *  USA
 */

import com.jbidwatcher.auction.AuctionEntry;

import java.util.Vector;
import javax.swing.JFrame;
import java.awt.event.ActionListener;

public abstract class MyActionListener implements ActionListener {
  protected Vector<AuctionEntry> m_entries;
  protected JFrame m_within;
  public void setEntries(Vector<AuctionEntry> allEntries) { m_entries = allEntries; }
  public void setFrame(JFrame jf) { m_within = jf; }
}