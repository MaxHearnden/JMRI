package jmri.jmrit.logixng.actions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.*;

import javax.annotation.Nonnull;

import jmri.*;
import jmri.jmrit.logixng.*;
import jmri.jmrit.logixng.util.ReferenceUtil;
import jmri.jmrit.logixng.util.parser.*;
import jmri.jmrit.logixng.util.parser.ExpressionNode;
import jmri.jmrit.logixng.util.parser.RecursiveDescentParser;
import jmri.util.ThreadingUtil;
import jmri.util.TypeConversionUtil;

/**
 * This action sets the state of a sensor.
 *
 * @author Daniel Bergqvist Copyright 2018
 */
public class ActionSensor extends AbstractDigitalAction implements VetoableChangeListener {

    private NamedBeanAddressing _addressing = NamedBeanAddressing.Direct;
    private NamedBeanHandle<Sensor> _sensorHandle;
    private String _reference = "";
    private String _localVariable = "";
    private String _formula = "";
    private ExpressionNode _expressionNode;
    private NamedBeanAddressing _stateAddressing = NamedBeanAddressing.Direct;
    private SensorState _sensorState = SensorState.Active;
    private String _stateReference = "";
    private String _stateLocalVariable = "";
    private String _stateFormula = "";
    private ExpressionNode _stateExpressionNode;

    public ActionSensor(String sys, String user)
            throws BadUserNameException, BadSystemNameException {
        super(sys, user);
    }

    @Override
    public Base getDeepCopy(Map<String, String> systemNames, Map<String, String> userNames) throws ParserException {
        DigitalActionManager manager = InstanceManager.getDefault(DigitalActionManager.class);
        String sysName = systemNames.get(getSystemName());
        String userName = userNames.get(getSystemName());
        if (sysName == null) sysName = manager.getAutoSystemName();
        ActionSensor copy = new ActionSensor(sysName, userName);
        copy.setComment(getComment());
        if (_sensorHandle != null) copy.setSensor(_sensorHandle);
        copy.setBeanState(_sensorState);
        copy.setAddressing(_addressing);
        copy.setFormula(_formula);
        copy.setLocalVariable(_localVariable);
        copy.setReference(_reference);
        copy.setStateAddressing(_stateAddressing);
        copy.setStateFormula(_stateFormula);
        copy.setStateLocalVariable(_stateLocalVariable);
        copy.setStateReference(_stateReference);
        return manager.registerAction(copy);
    }

    public void setSensor(@Nonnull String sensorName) {
        assertListenersAreNotRegistered(log, "setSensor");
        Sensor sensor = InstanceManager.getDefault(SensorManager.class).getSensor(sensorName);
        if (sensor != null) {
            setSensor(sensor);
        } else {
            removeSensor();
            log.warn("sensor \"{}\" is not found", sensorName);
        }
    }

    public void setSensor(@Nonnull NamedBeanHandle<Sensor> handle) {
        assertListenersAreNotRegistered(log, "setSensor");
        _sensorHandle = handle;
        InstanceManager.sensorManagerInstance().addVetoableChangeListener(this);
    }

    public void setSensor(@Nonnull Sensor sensor) {
        assertListenersAreNotRegistered(log, "setSensor");
        setSensor(InstanceManager.getDefault(NamedBeanHandleManager.class)
                .getNamedBeanHandle(sensor.getDisplayName(), sensor));
    }

    public void removeSensor() {
        assertListenersAreNotRegistered(log, "setSensor");
        if (_sensorHandle != null) {
            InstanceManager.sensorManagerInstance().removeVetoableChangeListener(this);
            _sensorHandle = null;
        }
    }

    public NamedBeanHandle<Sensor> getSensor() {
        return _sensorHandle;
    }

    public void setAddressing(NamedBeanAddressing addressing) throws ParserException {
        _addressing = addressing;
        parseFormula();
    }

    public NamedBeanAddressing getAddressing() {
        return _addressing;
    }

    public void setReference(@Nonnull String reference) {
        if ((! reference.isEmpty()) && (! ReferenceUtil.isReference(reference))) {
            throw new IllegalArgumentException("The reference \"" + reference + "\" is not a valid reference");
        }
        _reference = reference;
    }

    public String getReference() {
        return _reference;
    }

    public void setLocalVariable(@Nonnull String localVariable) {
        _localVariable = localVariable;
    }

    public String getLocalVariable() {
        return _localVariable;
    }

    public void setFormula(@Nonnull String formula) throws ParserException {
        _formula = formula;
        parseFormula();
    }

    public String getFormula() {
        return _formula;
    }

    private void parseFormula() throws ParserException {
        if (_addressing == NamedBeanAddressing.Formula) {
            Map<String, Variable> variables = new HashMap<>();

            RecursiveDescentParser parser = new RecursiveDescentParser(variables);
            _expressionNode = parser.parseExpression(_formula);
        } else {
            _expressionNode = null;
        }
    }

    public void setStateAddressing(NamedBeanAddressing addressing) throws ParserException {
        _stateAddressing = addressing;
        parseStateFormula();
    }

    public NamedBeanAddressing getStateAddressing() {
        return _stateAddressing;
    }

    public void setBeanState(SensorState state) {
        _sensorState = state;
    }

    public SensorState getBeanState() {
        return _sensorState;
    }

    public void setStateReference(@Nonnull String reference) {
        if ((! reference.isEmpty()) && (! ReferenceUtil.isReference(reference))) {
            throw new IllegalArgumentException("The reference \"" + reference + "\" is not a valid reference");
        }
        _stateReference = reference;
    }

    public String getStateReference() {
        return _stateReference;
    }

    public void setStateLocalVariable(@Nonnull String localVariable) {
        _stateLocalVariable = localVariable;
    }

    public String getStateLocalVariable() {
        return _stateLocalVariable;
    }

    public void setStateFormula(@Nonnull String formula) throws ParserException {
        _stateFormula = formula;
        parseStateFormula();
    }

    public String getStateFormula() {
        return _stateFormula;
    }

    private void parseStateFormula() throws ParserException {
        if (_stateAddressing == NamedBeanAddressing.Formula) {
            Map<String, Variable> variables = new HashMap<>();

            RecursiveDescentParser parser = new RecursiveDescentParser(variables);
            _stateExpressionNode = parser.parseExpression(_stateFormula);
        } else {
            _stateExpressionNode = null;
        }
    }

    @Override
    public void vetoableChange(java.beans.PropertyChangeEvent evt) throws java.beans.PropertyVetoException {
        if ("CanDelete".equals(evt.getPropertyName())) { // No I18N
            if (evt.getOldValue() instanceof Sensor) {
                if (evt.getOldValue().equals(getSensor().getBean())) {
                    PropertyChangeEvent e = new PropertyChangeEvent(this, "DoNotDelete", null, null);
                    throw new PropertyVetoException(Bundle.getMessage("Sensor_SensorInUseSensorActionVeto", getDisplayName()), e); // NOI18N
                }
            }
        } else if ("DoDelete".equals(evt.getPropertyName())) { // No I18N
            if (evt.getOldValue() instanceof Sensor) {
                if (evt.getOldValue().equals(getSensor().getBean())) {
                    removeSensor();
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Category getCategory() {
        return Category.ITEM;
    }

    private String getNewState() throws JmriException {

        switch (_stateAddressing) {
            case Reference:
                return ReferenceUtil.getReference(
                        getConditionalNG().getSymbolTable(), _stateReference);

            case LocalVariable:
                SymbolTable symbolTable = getConditionalNG().getSymbolTable();
                return TypeConversionUtil
                        .convertToString(symbolTable.getValue(_stateLocalVariable), false);

            case Formula:
                return _stateExpressionNode != null
                        ? TypeConversionUtil.convertToString(
                                _stateExpressionNode.calculate(
                                        getConditionalNG().getSymbolTable()), false)
                        : null;

            default:
                throw new IllegalArgumentException("invalid _addressing state: " + _stateAddressing.name());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void execute() throws JmriException {
        Sensor sensor;

//        System.out.format("ActionSensor.execute: %s%n", getLongDescription());

        switch (_addressing) {
            case Direct:
                sensor = _sensorHandle != null ? _sensorHandle.getBean() : null;
                break;

            case Reference:
                String ref = ReferenceUtil.getReference(
                        getConditionalNG().getSymbolTable(), _reference);
                sensor = InstanceManager.getDefault(SensorManager.class)
                        .getNamedBean(ref);
                break;

            case LocalVariable:
                SymbolTable symbolTable = getConditionalNG().getSymbolTable();
                sensor = InstanceManager.getDefault(SensorManager.class)
                        .getNamedBean(TypeConversionUtil
                                .convertToString(symbolTable.getValue(_localVariable), false));
                break;

            case Formula:
                sensor = _expressionNode != null ?
                        InstanceManager.getDefault(SensorManager.class)
                                .getNamedBean(TypeConversionUtil
                                        .convertToString(_expressionNode.calculate(
                                                getConditionalNG().getSymbolTable()), false))
                        : null;
                break;

            default:
                throw new IllegalArgumentException("invalid _addressing state: " + _addressing.name());
        }

//        System.out.format("ActionSensor.execute: sensor: %s%n", sensor);

        if (sensor == null) {
//            log.warn("sensor is null");
            return;
        }

        String name = (_stateAddressing != NamedBeanAddressing.Direct)
                ? getNewState() : null;

        SensorState state;
        if ((_stateAddressing == NamedBeanAddressing.Direct)) {
            state = _sensorState;
        } else {
            state = SensorState.valueOf(name);
        }

        ThreadingUtil.runOnLayoutWithJmriException(() -> {
            if (state == SensorState.Toggle) {
                if (sensor.getKnownState() == Sensor.INACTIVE) {
                    sensor.setCommandedState(Sensor.ACTIVE);
                } else {
                    sensor.setCommandedState(Sensor.INACTIVE);
                }
            } else {
                sensor.setCommandedState(state.getID());
            }
        });
    }

    @Override
    public FemaleSocket getChild(int index) throws IllegalArgumentException, UnsupportedOperationException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String getShortDescription(Locale locale) {
        return Bundle.getMessage(locale, "Sensor_Short");
    }

    @Override
    public String getLongDescription(Locale locale) {
        String namedBean;
        String state;

        switch (_addressing) {
            case Direct:
                String sensorName;
                if (_sensorHandle != null) {
                    sensorName = _sensorHandle.getBean().getDisplayName();
                } else {
                    sensorName = Bundle.getMessage(locale, "BeanNotSelected");
                }
                namedBean = Bundle.getMessage(locale, "AddressByDirect", sensorName);
                break;

            case Reference:
                namedBean = Bundle.getMessage(locale, "AddressByReference", _reference);
                break;

            case LocalVariable:
                namedBean = Bundle.getMessage(locale, "AddressByLocalVariable", _localVariable);
                break;

            case Formula:
                namedBean = Bundle.getMessage(locale, "AddressByFormula", _formula);
                break;

            default:
                throw new IllegalArgumentException("invalid _addressing state: " + _addressing.name());
        }

        switch (_stateAddressing) {
            case Direct:
                state = Bundle.getMessage(locale, "AddressByDirect", _sensorState._text);
                break;

            case Reference:
                state = Bundle.getMessage(locale, "AddressByReference", _stateReference);
                break;

            case LocalVariable:
                state = Bundle.getMessage(locale, "AddressByLocalVariable", _stateLocalVariable);
                break;

            case Formula:
                state = Bundle.getMessage(locale, "AddressByFormula", _stateFormula);
                break;

            default:
                throw new IllegalArgumentException("invalid _stateAddressing state: " + _stateAddressing.name());
        }

        return Bundle.getMessage(locale, "Sensor_Long", namedBean, state);
    }

    /** {@inheritDoc} */
    @Override
    public void setup() {
        // Do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void registerListenersForThisClass() {
    }

    /** {@inheritDoc} */
    @Override
    public void unregisterListenersForThisClass() {
    }

    /** {@inheritDoc} */
    @Override
    public void disposeMe() {
    }


    // This constant is only used internally in SensorState but must be outside
    // the enum.
    private static final int TOGGLE_ID = -1;


    public enum SensorState {
        Inactive(Sensor.INACTIVE, Bundle.getMessage("SensorStateInactive")),
        Active(Sensor.ACTIVE, Bundle.getMessage("SensorStateActive")),
        Toggle(TOGGLE_ID, Bundle.getMessage("SensorToggleStatus")),
        Unknown(Sensor.UNKNOWN, Bundle.getMessage("BeanStateUnknown")),
        Inconsistent(Sensor.INCONSISTENT, Bundle.getMessage("BeanStateInconsistent"));

        private final int _id;
        private final String _text;

        private SensorState(int id, String text) {
            this._id = id;
            this._text = text;
        }

        static public SensorState get(int id) {
            switch (id) {
                case Sensor.UNKNOWN:
                    return Unknown;

                case Sensor.INCONSISTENT:
                    return Inconsistent;

                case Sensor.INACTIVE:
                    return Inactive;

                case Sensor.ACTIVE:
                    return Active;

                case TOGGLE_ID:
                    return Toggle;

                default:
                    throw new IllegalArgumentException("invalid sensor state");
            }
        }

        public int getID() {
            return _id;
        }

        @Override
        public String toString() {
            return _text;
        }

    }

    /** {@inheritDoc} */
    @Override
    public void getUsageDetail(int level, NamedBean bean, List<NamedBeanUsageReport> report, NamedBean cdl) {
        log.debug("getUsageReport :: ActionSensor: bean = {}, cdl = {}, report = {}", bean, cdl, report);
        if (getSensor() != null && bean.equals(getSensor().getBean())) {
            report.add(new NamedBeanUsageReport("LogixNGAction", cdl, getLongDescription()));
        }
    }

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ActionSensor.class);
}
