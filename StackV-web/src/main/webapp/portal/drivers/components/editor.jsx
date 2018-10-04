import React from "react";
import PropTypes from "prop-types";
import convert from "xml-js";
import { Formik, Field, Form } from "formik";

class DriverEditor extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            advanced: false,
        };
    }

    render() {
        if (this.state.advanced) {
            return <div>{this.props.driver.xml}</div>;
        } else {
            let schema = JSON.parse(convert.xml2json(this.props.driver.xml, { compact: true, spaces: 4 }));

            let formElements = schema.driverInstance.properties.entry.map((entry) => <Field key={entry.key._text} type={entry.key._text} name={entry.key._text} />);
            return <Formik
                //initialValues={user /** { email, social } */}
                onSubmit={(values, actions) => {

                }}
                render={({ errors, touched, isSubmitting }) => (
                    <Form>
                        {formElements}
                        {status && status.msg && <div>{status.msg}</div>}
                        <button type="submit" disabled={true}>
                            Submit
                        </button>
                    </Form>
                )}
            />;
        }
    }
}
DriverEditor.propTypes = {
    driver: PropTypes.object.isRequired,
};
export default DriverEditor;