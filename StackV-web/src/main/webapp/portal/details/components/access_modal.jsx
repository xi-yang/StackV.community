import React from "react";
import PropTypes from "prop-types";

import UserPanel from "../../datatables/user_panel";

class AccessModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            opened: false,
            advanced: false,
        };
    }
    componentDidMount() {
        let modal = this;
    }

    render() {
        return <div className="modal fade" id="access-modal" style={{ top: "15%" }} data-backdrop={false}>
            <div className="modal-dialog" role="document">
                <div className="modal-content">
                    <div className="modal-header">
                        <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                        <h4 className="modal-title" id="access-modal-label">Share Access with Other Users</h4>
                    </div>
                    <div className="modal-body">
                        <UserPanel {...this.props} />
                    </div>
                    <div className="modal-footer">

                    </div>
                </div>
            </div>
        </div>;
    }
}
AccessModal.propTypes = {
};
export default AccessModal;
