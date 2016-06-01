import React from 'react';
import RaisedButton from 'material-ui/RaisedButton';
import Dialog from 'material-ui/Dialog';
import TextField from 'material-ui/TextField';
import {Card, CardHeader, CardActions} from 'material-ui/Card';
import Paper from 'material-ui/Paper';
import Toggle from 'material-ui/Toggle';
import {deepOrange500} from 'material-ui/styles/colors';
import FlatButton from 'material-ui/FlatButton';
import RefreshIndicator from 'material-ui/RefreshIndicator';
import getMuiTheme from 'material-ui/styles/getMuiTheme';
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider';
import ImageImage from 'material-ui/svg-icons/image/Image';
import ImageCompare from 'material-ui/svg-icons/image/Compare';
import AvMovie from 'material-ui/svg-icons/av/Movie';
import AvVideocam from 'material-ui/svg-icons/av/Videocam';
import ImageBurstMode from 'material-ui/svg-icons/image/burst-mode';
import Dropzone from 'react-dropzone';
import transitions from 'material-ui/styles/transitions';
import Checkbox from 'material-ui/Checkbox';
import {Tabs, Tab} from 'material-ui/Tabs';
import SwipeableViews from 'react-swipeable-views';

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
  },
  swipeableDiv: {
    padding: 10
  }
};

const muiTheme = getMuiTheme({
  palette: {
    accent1Color: deepOrange500,
  },
});

const serverPicsURL = "/seamcarvepics";
const serverVidURL = "/seamcarvevideo";


function objectEquality(objA, objB) {
  if ((objA === null || objA === undefined) && (objB !== null && objB !== undefined))
    return false;

  if ((objB === null || objB === undefined) && (objA !== null && objA !== undefined))
    return false;

  var keysA = Object.keys(objA);
  var keysB = Object.keys(objB);

  if (keysA.length != keysB.length)
    return false;

  for (var i = 0; i < keysA.length; i++) {
    var key = keysA[i];
    if (objA[key] !== objB[key])
      return false;
  }

  return true;
}


class Main extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      origImage : null,
      origImageWidth : 0,
      origImageHeight : 0,
      origImageName : "",

      seamRemoval : true,   // removes seams (otherwise inserts seams)
      heightWidthRatio : NaN,  // the ratio of height to width
      vertSeamsNum : NaN,
      horzSeamsNum : NaN,
      maxEnergy : NaN,

      // specifying results and whether or not they are open
      renderedImageOpen : false,
      energyOpen: false,
      makingOfOpen: false,

      imageResponse :
        {
          renderedImageSrc : null,
          energyImageSrc : null,
          request : null
        },
      videoResponse :
        {
          makingOfSrc: null,
          request : null
        },

      windowWidth : window.innerWidth,
      slideIndex : 0,   // which item in settings is selected
      isLoading : false   // server request showing loading signal
    };

    this.showRenderedImage = this.showRenderedImage.bind(this);
    this.hideRenderedImage = this.hideRenderedImage.bind(this);

    this.showEnergy = this.showEnergy.bind(this);
    this.hideEnergy = this.hideEnergy.bind(this);

    this.showMakingOf = this.showMakingOf.bind(this);
    this.hideMakingOf = this.hideMakingOf.bind(this);

    this.onDrop = this.onDrop.bind(this);

    this.handleResize = this.handleResize.bind(this);
    this.componentDidMount = this.componentDidMount.bind(this);
    this.handleSettingsChange = this.handleSettingsChange.bind(this);

    this.render = this.render.bind(this);
    this.serverRequest = this.serverRequest.bind(this);
    this.picOnSuccess = this.picOnSuccess.bind(this);
    this.vidOnSuccess = this.vidOnSuccess.bind(this);
    this.packageSendRequest = this.packageSendRequest.bind(this);
    this.openMakingOf = this.openMakingOf.bind(this);
    this.openImageEnergy = this.openImageEnergy.bind(this);
    this.openRenderedPicture = this.openRenderedPicture.bind(this);
  }

  componentDidMount() {
    window.addEventListener('resize', this.handleResize);
  }

  handleResize(e) {
    this.setState({windowWidth: window.innerWidth});
  }

  // when an image is dropped or chosen
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

  showRenderedImage() { this.setState({renderedImageOpen : true}); }
  hideRenderedImage() { this.setState({renderedImageOpen : false}); }

  showEnergy() { this.setState({energyOpen : true}); }
  hideEnergy() { this.setState({energyOpen : false}); }

  showMakingOf() { this.setState({makingOfOpen : true}); }
  hideMakingOf() { this.setState({makingOfOpen : false}); }

  // returns error if curval is outside of minval and maxval
  dimensionError(curVal, minVal, maxVal, error) {
    return ((curVal !== NaN) && (curVal < minVal || (maxVal !== NaN && curVal > maxVal))) ? error : null;
  }

  // changes the chosen slid element
  handleSettingsChange(value) {
    this.setState({
      slideIndex: value,
    });
  }

  // sends an ajax request of json (also shows loading signal)
  serverRequest(serverURL, request, onSuccess) {
    this.setState({isLoading: true});
    var requestStr = JSON.stringify(request);
    var that = this;
    $.ajax({
      headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
      },
      type: "POST",
      url: serverURL,
      data: requestStr,
      success: function(response) {
        onSuccess(request, response);
        that.setState({isLoading: false})
      },
      error: function(xhr, textStatus, errorMessage) {
        console.log("there was an error ", textStatus, errorMessage, xhr);
        that.setState({isLoading: false})
      },
      dataType: "json"
    });
  }

  // on success, sets up proper items for rendered image or energy image
  picOnSuccess(request, response) {
    this.setState({
      imageResponse : {
        renderedImageSrc : r.finalImage,
        energyImageSrc : r.energyImage,
        request : request
      }
    });
  }

  // parses video from request
  vidOnSuccess(request, response) {
    var frameRate = 30;
    var encoder = new Whammy.Video(frameRate);
    r.forEach(function(canvas) { encoder.add(canvas); });
    var output = encoder.compile();
    // the video url
    var video = (window.webkitURL || window.URL).createObjectURL(output);
    this.setState({
      videoResponse : {
        makingOfSrc : video,
        request : request
      }
    });
  }

  // packages response
  // onsuccessfunct is a function taking (request, response) argument
  // prevRequest (this function compares current request against previous request and returns cache if ===)
  // when all done, calls on Finish callback
  packageSendRequest(onSuccessFunct, serverURL, prevRequest, onFinishCallback) {
    var that = this;
    var canvas = document.createElement("canvas");
    canvas.width = origWidth;
    canvas.height = origHeight;
    var ctx = canvas.getContext("2d");
    var img = new Image();

    img.onload = function () {
      ctx.drawImage(img, 0, 0);
      var request = {};
      request.image = canvas.toDataURL();

      // if ratioed settings
      if (that.state.slideIndex == 1) {
        request.heightWidthRatio = that.state.heightWidthRatio;
        request.seamRemoval = that.state.seamRemoval;
      }
      // all settings
      else if (that.state.slideIndex == 2) {
        request.maxEnergy = that.state.maxEnergy;
        request.vertSeamNum = that.state.vertSeamNum;
        request.horzSeamNum = that.state.horzSeamNum;
      }

      // if the previous request doesn't equal this request, pull from server
      if (objectEquality(request, prevRequest)) {
        onFinishCallback();
      }
      else {
        serverRequest(serverURL, request,
          function(request, response) {
            onSuccessFunct(request, response);
            onFinishCallback();
          });
      }
    }
    img.src = imgSrc;
  }


  openRenderedPicture() {
    var that = this;
    packageSendRequest(picOnSuccess, serverPicsURL, this.state.imageResponse.request,
      function() { that.state.renderedImageOpen = true; });
  }

  openImageEnergy() {
    var that = this;
    packageSendRequest(picOnSuccess, serverPicsURL, this.state.imageResponse.request,
      function() { that.state.energyOpen = true; });
  }

  openMakingOf() {
    var that = this;
    packageSendRequest(vidOnSuccess, serverVidURL, this.state.videoResponse.request,
      function() { that.state.makingOfOpen = true; });
  }


  render() {
    const that = this;

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

    var winWidth = this.state.windowWidth;
    var refreshStatus = this.state.isLoading ? "loading" : "hide";

    return (
      <MuiThemeProvider muiTheme={muiTheme}>
      <div>
        <Dialog
          open={this.state.renderedImageOpen}
          title="Rendered Image"
          actions={renderedImageButton}
          onRequestClose={this.hideRenderedImage} >
          <img src={this.state.imageResponse.renderedImageSrc} />
        </Dialog>
        <Dialog
          open={this.state.energyOpen}
          title="Image Energy"
          actions={energyButton}
          onRequestClose={this.hideEnergy} >
          <img src={this.state.imageResponse.energyImageSrc} />
        </Dialog>
        <Dialog
          open={this.state.makingOfOpen}
          title="The Whole Process"
          actions={makingOfButton}
          onRequestClose={this.hideMakingOf} >
          <video controls>
            {/* TODO change video/mp4 based on what you get */}
            <source src={this.state.videoResponse.makingOfSrc} />
            Your browser does not support HTML5 video.
          </video>
        </Dialog>
        <RefreshIndicator
          size={60}
          left={winWidth / 2 - 30}
          top={50}
          status={refreshStatus}
          style={{ position: "fixed" }}
        />
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

          <Tabs
            onChange={this.handleSettingsChange}
            value={this.state.slideIndex}
          >
            <Tab label="Default" value={0} />
            <Tab label="Ratio" value={1} />
            <Tab label="Advanced" value={2} />
          </Tabs>
          <SwipeableViews
            index={this.state.slideIndex}
            onChangeIndex={this.handleChange}
          >
            <div style={styles.swipeableDiv}>
              Remove the default vertical and horizontal seams from the image.  By default, it removes 10% vertically and horizontally.
            </div>
            <div style={styles.swipeableDiv}>
              Remove or insert seams to achieve a particular height to width ratio.
              <Checkbox label="Remove seams" />
              <TextField
                style={styles.textfield}
                floatingLabelText="Ratio of height to width"
                type="number"
              />
            </div>
            <div style={styles.swipeableDiv}>
              All options for the seam carving process.
              <Checkbox label="Remove seams" />
              <div>
                <TextField
                  style={styles.textfield}
                  floatingLabelText="Vertical seams to remove"
                  type="number"
                />
                <TextField
                  style={styles.textfield}
                  floatingLabelText="Horizontal seams to remove"
                  type="number"
                />
              </div>
              <TextField
                style={styles.textfield}
                floatingLabelText="Maximum pixel energy"
                type="number"
              />
            </div>
          </SwipeableViews>
        </Card>
        <Card style={styles.card}>
          <h2>Get Results</h2>
          <FlatButton
            icon={<ImageBurstMode/>}
            label="See Processed Image"
            onTouchTap={this.openRenderedPicture}
            primary={true}
          />
          <FlatButton
            label="View Image Energy"
            onTouchTap={this.openImageEnergy}
            icon={<ImageCompare />}
          />
          <FlatButton
            label="Watch the Process (might take a bit)"
            onTouchTap={this.openMakingOf}
            icon={<AvVideocam/>}
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
