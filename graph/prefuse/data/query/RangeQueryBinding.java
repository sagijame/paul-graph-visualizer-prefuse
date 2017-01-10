package prefuse.data.query;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import prefuse.data.expression.ColumnExpression;
import prefuse.data.expression.Literal;
import prefuse.data.expression.RangePredicate;
import prefuse.data.tuple.TupleSet;
import prefuse.util.DataLib;
import prefuse.util.TypeLib;
import prefuse.util.ui.JRangeSlider;
import prefuse.util.ui.ValuedRangeModel;

/**
 * DynamicQueryBinding supporting queries based on a range of included
 * data values.
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class RangeQueryBinding extends DynamicQueryBinding {

    private Class<?> m_type;
    private Listener m_lstnr;
    private ValuedRangeModel m_model;
    private boolean m_ordinal;
    
    private static FocusListener s_sliderAdj;
    
    /**
     * Create a new RangeQueryBinding over the given set and data field.
     * @param ts the TupleSet to query
     * @param field the data field (Table column) to query
     */
    public RangeQueryBinding(TupleSet ts, String field) {
        this(ts, field, false);
    }
    
    /**
     * Create a new RangeQueryBinding over the given set and data field,
     * optionally forcing an ordinal treatment of data.
     * @param ts the TupleSet to query
     * @param field the data field (Table column) to query
     * @param forceOrdinal if true, forces all items in the range to be
     * treated in strictly ordinal fashion. That means that if the data
     * is numerical, the quantitative nature of the data will be ignored
     * and only the relative ordering of the numbers will matter. In terms
     * of mechanism, this entails that a {@link ObjectRangeModel} and not
     * a {@link NumberRangeModel} will be used to represent the data. If
     * the argument is false, default inference mechanisms will be used.
     */
    public RangeQueryBinding(TupleSet ts, String field, boolean forceOrdinal) {
        super(ts, field);
        m_type = DataLib.inferType(ts, field);
        m_ordinal = forceOrdinal;
        m_lstnr = new Listener();
        initPredicate();
        initModel();
    }
    
    private void initPredicate() {
        // determine minimum and maximum values
        Object min = DataLib.min(m_tuples, m_field).get(m_field);
        Object max = DataLib.max(m_tuples, m_field).get(m_field);
        
        // set up predicate
        Literal left = Literal.getLiteral(min, m_type);
        Literal right = Literal.getLiteral(max, m_type);
        ColumnExpression ce = new ColumnExpression(m_field);
        RangePredicate rp = new RangePredicate(ce, left, right);
        setPredicate(rp);
    }
    
    public void initModel() {
        if ( m_model != null )
            m_model.removeChangeListener(m_lstnr);
        
        // set up data / selection model
        ValuedRangeModel model = null;
        if ( TypeLib.isNumericType(m_type) && !m_ordinal ) {
            Number min = (Number)DataLib.min(m_tuples, m_field).get(m_field);
            Number max = (Number)DataLib.max(m_tuples, m_field).get(m_field);
            model = new NumberRangeModel(min, max, min, max);
        } else {
            model = new ObjectRangeModel(
                        DataLib.ordinalArray(m_tuples, m_field));
        }
        m_model = model;
        m_model.addChangeListener(m_lstnr);
    }

    /**
     * Return the ValuedRangeModel constructed by this dynamic query binding.
     * This model backs any user interface components generated by this
     * instance.
     * @return the ValuedRangeModel for this range query binding.
     */
    public ValuedRangeModel getModel() {
        return m_model;
    }
    
    /**
     * Attempts to return the ValuedRangeModel for this binding as a 
     * NumberRangeModel. If the range model is not an instance of
     * {@link NumberRangeModel}, a null value is returned.
     * @return the ValuedRangeModel for this binding as a 
     * {@link NumberRangeModel}, or null if the range is not numerical.
     */
    public NumberRangeModel getNumberModel() {
        return (m_model instanceof NumberRangeModel ? 
                (NumberRangeModel)m_model : null);
    }
    
    /**
     * Attempts to return the ValuedRangeModel for this binding as an 
     * ObjectRangeModel. If the range model is not an instance of
     * {@link ObjectRangeModel}, a null value is returned.
     * @return the ValuedRangeModel for this binding as an
     * {@link ObjectRangeModel}, or null if the range is numerical.
     */
    public ObjectRangeModel getObjectModel() {
        return (m_model instanceof ObjectRangeModel ? 
                (ObjectRangeModel)m_model : null);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Create a new horizontal range slider for interacting with the query.
     * @return a {@link prefuse.util.ui.JRangeSlider} bound to this dynamic
     * query.
     * @see prefuse.data.query.DynamicQueryBinding#createComponent()
     */
    public JComponent createComponent() {
        return createHorizontalRangeSlider();
    }
    
    /**
     * Create a new horizontal range slider for interacting with the query.
     * @return a {@link prefuse.util.ui.JRangeSlider} bound to this dynamic
     * query.
     */
    public JRangeSlider createHorizontalRangeSlider() {
        return createRangeSlider(JRangeSlider.HORIZONTAL, 
                JRangeSlider.LEFTRIGHT_TOPBOTTOM);
    }

    /**
     * Create a new vertical range slider for interacting with the query.
     * @return a {@link prefuse.util.ui.JRangeSlider} bound to this dynamic
     * query.
     */
    public JRangeSlider createVerticalRangeSlider() {
        return createRangeSlider(JRangeSlider.VERTICAL, 
                JRangeSlider.RIGHTLEFT_BOTTOMTOP);
    }
    
    /**
     * Create a new range slider for interacting with the query, using the
     * given orientation and direction.
     * @param orientation the orientation (horizontal or vertical) of the
     * slider (see {@link prefuse.util.ui.JRangeSlider})
     * @param direction the direction (direction of data values) of the slider 
     * (see {@link prefuse.util.ui.JRangeSlider})
     * @return a {@link prefuse.util.ui.JRangeSlider} bound to this dynamic
     * query.
     */
    public JRangeSlider createRangeSlider(int orientation, int direction) {
        return new JRangeSlider(m_model, orientation, direction);
    }
    
    /**
     * Create a new regular (non-range) slider for interacting with the query.
     * This allows you to select a single value at a time.
     * @return a {@link javax.swing.JSlider} bound to this dynamic query.
     */
    public JSlider createSlider() {
        JSlider slider = new JSlider(m_model);
        slider.addFocusListener(getSliderAdjuster());
        return slider;
    }
    
    private synchronized static FocusListener getSliderAdjuster() {
        if ( s_sliderAdj == null )
            s_sliderAdj = new SliderAdjuster();
        return s_sliderAdj;
    }
    
    // ------------------------------------------------------------------------
    
    private static class SliderAdjuster implements FocusListener {
        public void focusGained(FocusEvent e) {
            ((JSlider)e.getSource()).setExtent(0);
        }
        public void focusLost(FocusEvent e) {
            // do nothing
        }
    } // end of inner class SliderAdjuster
    
    private class Listener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            ValuedRangeModel model = (ValuedRangeModel)e.getSource();
            Object lo = model.getLowValue();
            Object hi = model.getHighValue();
            
            RangePredicate rp = (RangePredicate)m_query;
            rp.setLeftExpression(Literal.getLiteral(lo, m_type));
            rp.setRightExpression(Literal.getLiteral(hi, m_type));
        }
    } // end of inner class Listener

} // end of class RangeQueryBinding
