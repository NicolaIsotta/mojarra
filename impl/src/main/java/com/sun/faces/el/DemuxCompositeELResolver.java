/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.faces.el;

import java.beans.FeatureDescriptor;

import java.util.Iterator;

import java.util.NoSuchElementException;

import jakarta.el.ELResolver;
import jakarta.el.ELContext;
import jakarta.el.ELException;

/**
 * Maintains an ordered composite list of child <code>ELResolver for JSF</code>.
 *
 */
public class DemuxCompositeELResolver extends FacesCompositeELResolver {
    private final ELResolverChainType _chainType;

    private ELResolver[] _rootELResolvers = new ELResolver[2];
    private ELResolver[] _propertyELResolvers = new ELResolver[2];
    private ELResolver[] _allELResolvers = new ELResolver[2];

    private int _rootELResolverCount = 0;
    private int _propertyELResolverCount = 0;
    private int _allELResolverCount = 0;

    public DemuxCompositeELResolver(ELResolverChainType chainType) {
        if (chainType == null) {
            throw new NullPointerException();
        }

        _chainType = chainType;
    }

    @Override
    public ELResolverChainType getChainType() {
        return _chainType;
    }

    private void _addAllELResolver(ELResolver elResolver) {
        if (elResolver == null) {
            throw new NullPointerException();
        }

        // grow array, if necessary
        if (_allELResolverCount == _allELResolvers.length) {
            ELResolver[] biggerResolvers = new ELResolver[_allELResolverCount * 2];
            System.arraycopy(_allELResolvers, 0, biggerResolvers, 0, _allELResolverCount);
            _allELResolvers = biggerResolvers;
        }

        // assign new resolver to end
        _allELResolvers[_allELResolverCount] = elResolver;
        _allELResolverCount++;
    }

    private void _addRootELResolver(ELResolver elResolver) {
        if (elResolver == null) {
            throw new NullPointerException();
        }

        // grow array, if necessary
        if (_rootELResolverCount == _rootELResolvers.length) {
            ELResolver[] biggerResolvers = new ELResolver[_rootELResolverCount * 2];
            System.arraycopy(_rootELResolvers, 0, biggerResolvers, 0, _rootELResolverCount);
            _rootELResolvers = biggerResolvers;
        }

        // assign new resolver to end
        _rootELResolvers[_rootELResolverCount] = elResolver;
        _rootELResolverCount++;
    }

    public void _addPropertyELResolver(ELResolver elResolver) {
        if (elResolver == null) {
            throw new NullPointerException();
        }

        // grow array, if necessary
        if (_propertyELResolverCount == _propertyELResolvers.length) {
            ELResolver[] biggerResolvers = new ELResolver[_propertyELResolverCount * 2];
            System.arraycopy(_propertyELResolvers, 0, biggerResolvers, 0, _propertyELResolverCount);
            _propertyELResolvers = biggerResolvers;
        }

        // assign new resolver to end
        _propertyELResolvers[_propertyELResolverCount] = elResolver;
        _propertyELResolverCount++;
    }

    @Override
    public void addRootELResolver(ELResolver elResolver) {
        // pass ELResolver to CompositeELResolver so that J2EE6 invoke() method works. Once we can
        // have a compile dependency on J2EE6, we can override invoke() ourselves and remove this.
        super.add(elResolver);

        _addRootELResolver(elResolver);
        _addAllELResolver(elResolver);
    }

    @Override
    public void addPropertyELResolver(ELResolver elResolver) {
        // pass ELResolver to CompositeELResolver so that J2EE6 invoke() method works. Once we can
        // have a compile dependency on J2EE6, we can override invoke() ourselves and remove this.
        super.add(elResolver);

        _addPropertyELResolver(elResolver);
        _addAllELResolver(elResolver);
    }

    @Override
    public void add(ELResolver elResolver) {
        // pass ELResolver to CompositeELResolver so that J2EE6 invoke() method works. Once we can
        // have a compile dependency on J2EE6, we can override invoke() ourselves and remove this.
        super.add(elResolver);

        _addRootELResolver(elResolver);
        _addPropertyELResolver(elResolver);
        _addAllELResolver(elResolver);
    }

    private Object _getValue(int resolverCount, ELResolver[] resolvers, ELContext context, Object base, Object property) throws ELException {
        for (int i = 0; i < resolverCount; i++) {
            Object result = resolvers[i].getValue(context, base, property);

            if (context.isPropertyResolved()) {
                return result;
            }
        }

        return null;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) throws ELException {
        context.setPropertyResolved(false);

        int resolverCount;
        ELResolver[] resolvers;

        if (base == null) {
            resolverCount = _rootELResolverCount;
            resolvers = _rootELResolvers;
        } else {
            resolverCount = _propertyELResolverCount;
            resolvers = _propertyELResolvers;
        }

        return _getValue(resolverCount, resolvers, context, base, property);
    }

    private Class<?> _getType(int resolverCount, ELResolver[] resolvers, ELContext context, Object base, Object property) throws ELException {
        for (int i = 0; i < resolverCount; i++) {
            Class<?> type = resolvers[i].getType(context, base, property);

            if (context.isPropertyResolved()) {
                return type;
            }
        }

        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) throws ELException {
        context.setPropertyResolved(false);

        int resolverCount;
        ELResolver[] resolvers;

        if (base == null) {
            resolverCount = _rootELResolverCount;
            resolvers = _rootELResolvers;
        } else {
            resolverCount = _propertyELResolverCount;
            resolvers = _propertyELResolvers;
        }

        return _getType(resolverCount, resolvers, context, base, property);
    }

    private void _setValue(int resolverCount, ELResolver[] resolvers, ELContext context, Object base, Object property, Object val) throws ELException {
        for (int i = 0; i < resolverCount; i++) {
            resolvers[i].setValue(context, base, property, val);

            if (context.isPropertyResolved()) {
                return;
            }
        }
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object val) throws ELException {
        context.setPropertyResolved(false);

        int resolverCount;
        ELResolver[] resolvers;

        if (base == null) {
            resolverCount = _rootELResolverCount;
            resolvers = _rootELResolvers;
        } else {
            resolverCount = _propertyELResolverCount;
            resolvers = _propertyELResolvers;
        }

        _setValue(resolverCount, resolvers, context, base, property, val);
    }

    private boolean _isReadOnly(int resolverCount, ELResolver[] resolvers, ELContext context, Object base, Object property) throws ELException {
        for (int i = 0; i < resolverCount; i++) {
            boolean isReadOnly = resolvers[i].isReadOnly(context, base, property);

            if (context.isPropertyResolved()) {
                return isReadOnly;
            }
        }

        return false;
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) throws ELException {
        context.setPropertyResolved(false);

        int resolverCount;
        ELResolver[] resolvers;

        if (base == null) {
            resolverCount = _rootELResolverCount;
            resolvers = _rootELResolvers;
        } else {
            resolverCount = _propertyELResolverCount;
            resolvers = _propertyELResolvers;
        }

        return _isReadOnly(resolverCount, resolvers, context, base, property);
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return new DescriptorIterator(context, base, _allELResolvers, _allELResolverCount);
    }

    private final static class DescriptorIterator implements Iterator<FeatureDescriptor> {
        // snapshot the ELResolver array to avoid using a non-static inner class that needs to
        // make function calls
        public DescriptorIterator(ELContext context, Object base, ELResolver[] resolvers, int resolverCount) {
            _context = context;
            _base = base;
            _resolvers = resolvers;
            _resolverCount = resolverCount;
        }

        @Override
        public boolean hasNext() {
            do {
                // A null return does *not* mean hasNext() should return false.
                Iterator<FeatureDescriptor> currIterator = _getCurrIterator();

                if (null != currIterator) {
                    if (currIterator.hasNext()) {
                        return true;
                    } else {
                        _currIterator = null;
                        _currResolverIndex++;
                    }
                } else {
                    if (_currResolverIndex < _resolverCount) {
                        continue;
                    } else {
                        return false;
                    }
                }

            } while (true);
        }

        private Iterator<FeatureDescriptor> _getCurrIterator() {
            Iterator<FeatureDescriptor> currIterator = _currIterator;

            if (currIterator == null) {
                if (_currResolverIndex < _resolverCount) {
                    currIterator = _resolvers[_currResolverIndex].getFeatureDescriptors(_context, _base);
                    _currResolverIndex++;
                    _currIterator = currIterator;
                }
            }

            return currIterator;
        }

        @Override
        public FeatureDescriptor next() {
            if (hasNext()) {
                return _getCurrIterator().next();
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private final ELContext _context;
        private final Object _base;
        private final ELResolver[] _resolvers;
        private final int _resolverCount;
        private int _currResolverIndex;
        private Iterator<FeatureDescriptor> _currIterator;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return null;
    }
}
