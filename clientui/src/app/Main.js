import React from 'react';
import RaisedButton from 'material-ui/RaisedButton';
import Dialog from 'material-ui/Dialog';
import TextField from 'material-ui/TextField';
import {Card, CardHeader, CardActions} from 'material-ui/Card';
import Paper from 'material-ui/Paper';
import Toggle from 'material-ui/Toggle';
import {deepOrange500} from 'material-ui/styles/colors';
import FlatButton from 'material-ui/FlatButton';
import getMuiTheme from 'material-ui/styles/getMuiTheme';
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider';
import ImageImage from 'material-ui/svg-icons/image/Image';
import ImageCompare from 'material-ui/svg-icons/image/Compare';
import AvMovie from 'material-ui/svg-icons/av/Movie';
import AvVideocam from 'material-ui/svg-icons/av/Videocam';
import ImageBurstMode from 'material-ui/svg-icons/image/burst-mode';
import Dropzone from 'react-dropzone';
import transitions from 'material-ui/styles/transitions';
import Whammy from 'whammy/whammy'

const styles = {
  card: {
    padding: 10,
    marginTop: 10,
    marginBottom: 10
  },
  imagePaper: {
    padding: 10,
    margin: 10,
    display: 'inline-block',
    textAlign: 'center',
    verticalAlign: 'middle'
  },
  dropzonePaper: {
    padding: 10,
    margin: 10,
    marginRight: 50,
    display: 'inline-block',
    textAlign: 'center',
    verticalAlign: 'middle'
  },
  textfield: {
    margin: 10
  },
  settings: {
    overflow: 'auto',
    maxHeight: 1400,
    transition: transitions.create('max-height', '800ms', '0ms', 'ease-in-out'),
    marginTop: 5,
    marginBottom: 5,
  },
  settingsRetracted: {
    overflow: 'auto',
    maxHeight: 0,
    transition: transitions.create('max-height', '800ms', '0ms', 'ease-in-out'),
    marginTop: 5,
    marginBottom: 5,
  },
  dropzone: {
    padding: 10,
  },
  imgpreview: {
    maxHeight: 200
  }
};

const muiTheme = getMuiTheme({
  palette: {
    accent1Color: deepOrange500,
  },
});

const serverURL = "/seamcarve"

class Main extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      origImage : null,
      origImageWidth : 0,
      origImageHeight : 0,
      origImageName : "",
      renderedImageOpen : false,
      energyOpen: false,
      makingOfOpen: false,
      renderedImageSrc : null,
      energyImageSrc: null,
      makingOfSrc: null,
      expandedSettings : false,
      heightFieldValue : NaN,
      widthFieldValue : NaN,
      horzSeamsFieldValue : NaN,
      vertSeamsFieldValue : NaN
    };
    this.showRenderedImage = this.showRenderedImage.bind(this);
    this.hideRenderedImage = this.hideRenderedImage.bind(this);

    this.showEnergy = this.showEnergy.bind(this);
    this.hideEnergy = this.hideEnergy.bind(this);

    this.showMakingOf = this.showMakingOf.bind(this);
    this.hideMakingOf = this.hideMakingOf.bind(this);

    this.onToggleSettings = this.onToggleSettings.bind(this);

    this.onDrop = this.onDrop.bind(this);
    this.resetProcessed = this.resetProcessed.bind(this);
    this.reset = this.reset.bind(this);
    this.render = this.render.bind(this);
    this.processImage = this.processImage.bind(this);
  }


  // resets image and processed elements
  resetProcessed() {
    this.setState({
      renderedImageSrc : null,
      energyImageSrc : null,
      makingOfSrc : null
    });
  }

  // resets image and processed elements
  reset() {
    this.setState({
      origImage : null,
    });
    this.resetProcessed();
  }

  // this is where the magic happens...sets up processed image, image energy, video
  processImage(imgSrc, origWidth, origHeight, expandedSettings, imgWidth, imgHeight, vertSeams, horzSeams) {
    var that = this;
    console.log(origWidth, origHeight, expandedSettings, imgWidth, imgHeight, vertSeams, horzSeams);
    var canvas = document.createElement("canvas");
    canvas.width = origWidth;
    canvas.height = origHeight;
    var ctx = canvas.getContext("2d");

    var img = new Image();
    img.onload = function () {
      ctx.drawImage(img, 0, 0);
      var request = {};
      request.image = canvas.toDataURL();
      var requestStr = JSON.stringify(request);

      // TODO handle other parameters
      $.ajax({
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        type: "POST",
        url: serverURL,
        data: requestStr,
        success: function(response) {
          console.log("response received: ", response);
          that.setState({renderedImageSrc: response.finalImage });
          that.setState({energyImageSrc: response.energyImage });
        },
        error: function(xhr, textStatus, errorMessage) {
          console.log("there was an error ", textStatus, errorMessage, xhr);
        },
        dataType: "json"
      });


      // TODO handle video???
    }
    img.src = imgSrc;
  }

  createVideo(imgs) {
    var frameRate = 30;
    var encoder = new Whammy.Video(frameRate);
    imgs.forEach(function(canvas) { encoder.add(canvas); });
    var output = encoder.compile();
    // the video url
    return (window.webkitURL || window.URL).createObjectURL(output);
  }

  onDrop(files) {
    var that = this;
    that.reset();
    var fr = new FileReader();
    fr.onload = function() {
        var img = new Image();
        img.onload = function() {
          that.setState({
            origImage : fr.result,  // the data url
            origImageName : files[0].name,
            origImageWidth : img.width,
            origImageHeight : img.height
          });
        };

        img.src = fr.result;
    };
    fr.readAsDataURL(files[0]);
  }

  onToggleSettings(e, isChecked) { this.setState({expandedSettings : !isChecked}); }

  showRenderedImage() { this.setState({renderedImageOpen : true}); }
  hideRenderedImage() { this.setState({renderedImageOpen : false}); }

  showEnergy() { this.setState({energyOpen : true}); }
  hideEnergy() { this.setState({energyOpen : false}); }

  showMakingOf() { this.setState({makingOfOpen : true}); }
  hideMakingOf() { this.setState({makingOfOpen : false}); }

  dimensionError(curVal, minVal, maxVal, error) {
    return ((curVal !== NaN) && (curVal < minVal || (maxVal !== NaN && curVal > maxVal))) ? error : null;
  }


  render() {
    const that = this;
    const processImage = function() {
      that.processImage(
        that.state.origImage,
        that.state.origImageWidth,
        that.state.origImageHeight,
        that.state.expandedSettings,
        that.state.widthFieldValue,
        that.state.heightFieldValue,
        that.state.vertSeamsFieldValue,
        that.state.horzSeamsFieldValue);
    }

    const heightError = this.dimensionError(that.state.heightFieldValue, 1, NaN,
      "The new image height has to be greater than 1");

    const widthError = this.dimensionError(that.state.widthFieldValue, 1, NaN,
      "The new image width has to be greater than 1");

    const horzSeamsError = this.dimensionError(that.state.horzSeamsFieldValue, 0, that.state.origImageHeight - 1,
      "Cannot remove negative seams and must remove less seams than the original image height");

    const vertSeamsError = this.dimensionError(that.state.vertSeamsFieldValue, 0, that.state.origImageWidth - 1,
      "Cannot remove negative seams and must remove less seams than the original image width");

    const heightFieldUpdate = function(e,v) { that.setState({heightFieldValue : parseInt(v, 10) }); }
    const widthFieldUpdate = function(e,v) { that.setState({widthFieldValue : parseInt(v, 10) }); }
    const horzSeamsFieldUpdate = function(e,v) { that.setState({horzSeamsFieldValue : parseInt(v, 10) }); }
    const vertSeamsFieldUpdate = function(e,v) { that.setState({vertSeamsFieldValue : parseInt(v, 10) }); }

    const renderedImageButton =
      (<FlatButton
        label="Ok"
        secondary={false}
        onTouchTap={this.hideRenderedImage}
      />);

    const energyButton =
      (<FlatButton
        label="Ok"
        secondary={false}
        onTouchTap={this.hideEnergy}
      />);

    const makingOfButton =
      (<FlatButton
        label="Ok"
        secondary={false}
        onTouchTap={this.hideMakingOf}
      />);

      var previewImage =
      (this.state.origImage === null) ?
        null :
        (<Paper style={styles.imagePaper} zDepth={3}>
          <img src={this.state.origImage} style={styles.imgpreview}/>
          <h3>{this.state.origImageName}</h3>
          <p>{this.state.origImageWidth} x {this.state.origImageHeight}</p>
        </Paper>);

    let settingsStyle = (this.state.expandedSettings) ? styles.settings : styles.settingsRetracted;

    return (
      <MuiThemeProvider muiTheme={muiTheme}>
      <div>
        <Dialog
          open={this.state.renderedImageOpen}
          title="Rendered Image"
          actions={renderedImageButton}
          onRequestClose={this.hideRenderedImage} >
          <img src={this.state.renderedImageSrc} />
        </Dialog>
        <Dialog
          open={this.state.energyOpen}
          title="Image Energy"
          actions={energyButton}
          onRequestClose={this.hideEnergy} >
          <img src={this.state.energyImageSrc} />
        </Dialog>
        <Dialog
          open={this.state.makingOfOpen}
          title="The Whole Process"
          actions={makingOfButton}
          onRequestClose={this.hideMakingOf} >
          <video controls>
            {/* TODO change video/mp4 based on what you get */}
            <source src={this.state.makingOfSrc} />
            Your browser does not support HTML5 video.
          </video>
        </Dialog>
        <h1>Seam Carving Picture Resizing</h1>
        <Card style={styles.card}>
            <h2>Image Upload</h2>
            <div>
              <Paper style={styles.dropzonePaper} zDepth={3}>
                <Dropzone onDrop={this.onDrop} multiple={false}>
                  <div style={styles.dropzone}>
                  <h2>Upload Image Here</h2>
                  <p>Drag and drop an image or click to upload</p>
                  </div>
                </Dropzone>
              </Paper>
              {previewImage}
            </div>
        </Card>
        <Card style={styles.card}>
          <h2>Settings</h2>
          <Toggle
            label="Use Default Settings"
            ref={(ref) => this.defaultSettings = ref}
            onToggle={this.onToggleSettings}
            defaultToggled={true}
            labelPosition="right"
            disabled={this.state.origImage === null}
          />
          <div style={settingsStyle} >
            <div>
              <TextField
                style={styles.textfield}
                floatingLabelText="Desired Image Width"
                onChange={widthFieldUpdate}
                errorText={widthError}
                type="number"
                disabled={this.state.origImage === null}
              />
              <TextField
                style={styles.textfield}
                floatingLabelText="Desired Image Height"
                onChange={heightFieldUpdate}
                errorText={heightError}
                type="number"
                disabled={this.state.origImage === null}
              />
            </div>
            <div>
              <TextField
                style={styles.textfield}
                floatingLabelText="Vertical Number of Seams"
                onChange={vertSeamsFieldUpdate}
                errorText={vertSeamsError}
                type="number"
                disabled={this.state.origImage === null}
              />
              <TextField
                style={styles.textfield}
                floatingLabelText="Horizontal Number of Seams"
                onChange={horzSeamsFieldUpdate}
                errorText={horzSeamsError}
                type="number"
                disabled={this.state.origImage === null}
              />
            </div>
          </div>
          <FlatButton
            icon={<ImageBurstMode/>}
            label="Process Image"
            onTouchTap={processImage}
            disabled={this.state.origImage === null}
          />
        </Card>
        <Card style={styles.card}>
          <h2>Results</h2>
          <FlatButton
            label="View Rendered Image"
            onTouchTap={this.showRenderedImage}
            icon={<ImageImage />}
            default={true}
            disabled={this.state.renderedImageSrc === null}
          />
          <FlatButton
            label="View Image Energy"
            onTouchTap={this.showEnergy}
            icon={<ImageCompare />}
            primary={true}
            disabled={this.state.energyImageSrc === null}
          />
          <FlatButton
            label="Watch the Process"
            onTouchTap={this.showMakingOf}
            icon={<AvVideocam/>}
            primary={true}
            disabled={this.state.makingOfSrc === null}
          />
        </Card>
        <Card style={styles.card}>
          <h2>Explanation</h2>
          <FlatButton
            label="See the video discussing seam carving"
            icon={<AvMovie />}
            linkButton={true}
            href="https://www.youtube.com/watch?v=QLuPakjrSDM"
            secondary={true}
          />
        </Card>
      </div>
      </MuiThemeProvider>
    );
  }
}

export default Main;
