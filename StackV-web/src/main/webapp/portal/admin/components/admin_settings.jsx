import React from "react";
import ReactInterval from "react-interval";

class SettingsPanel extends React.Component {
    constructor(props) {
        super(props);

        this.loadSettings = this.loadSettings.bind(this);
    }
    componentDidMount() {
        this.loadSettings();
    }

    render() {
        //<ReactInterval timeout={this.props.refreshTimer} enabled={this.props.refreshEnabled} callback={this.loadSettings} />;
        return <div className={this.props.active ? "top" : "bottom"} id="settings-panel">
            <div id="settings-header-div">Global Settings</div>
            <div id="settings-body-div">
                <label>Test Setting 1<input className="form-control" name="testsetting1"></input></label>
                <label>Test Setting 2<input className="form-control" name="testsetting2"></input></label>
            </div>
        </div>;
    }

    loadSettings() {

    }
}
export default SettingsPanel;
