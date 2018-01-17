/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 *
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.attributes.base.impl;

import de.ovgu.featureide.fm.attributes.base.IFeatureAttribute;
import de.ovgu.featureide.fm.core.base.IFeature;

/**
 * TODO description
 *
 * @author Joshua Sprey
 * @author Chico Sundermann
 */
public class BooleanFeatureAttribute extends FeatureAttribute {

	private Boolean value;

	/**
	 * @param name
	 * @param unit
	 * @param value
	 * @param recursive
	 * @param configureable
	 */
	public BooleanFeatureAttribute(IFeature feature, String name, String unit, Boolean value, boolean recursive, boolean configurable) {
		super(feature, name, unit, recursive, configurable);
		this.value = value;
		attributeType = FeatureAttribute.BOOLEAN;
	}

	@Override
	public Boolean getValue() {
		return value;
	}

	/*
	 * (non-Javadoc)
	 * @see de.ovgu.featureide.fm.core.attributes.impl.FeatureAttribute#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(Object value) {
		if (value == null) {
			this.value = false;
			return;
		}
		if (value instanceof Boolean) {
			this.value = (Boolean) value;
		}
	}

	/**
	 * Returns a copy of the attribute
	 */
	@Override
	public IFeatureAttribute cloneAtt() {
		return new BooleanFeatureAttribute(this.getFeature(), this.getName(), this.getUnit(), this.getValue(), this.isRecursive(), this.isConfigurable());
	}

}
