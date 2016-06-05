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
import ImageBlurOff from 'material-ui/svg-icons/image/blur-off';
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

import Whammy from 'whammy/whammy';

var $ = require('jquery');
var fancybox = require('fancybox')($);



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
  },
  imageDivStyle: {
    'background-repeat': 'no-repeat',
    'background-position': 'center center',
    'background-size': 'contain'
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
          seamImageSrc : null,
          request : null
        },
      videoResponse :
        {
          makingOfSrc: null,
          request : null
        },

      windowWidth : window.innerWidth,
      slideIndex : 0,   // which item in settings is selected
      isLoading : false,   // server request showing loading signal

      errorAlert : []
    };

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
    this.openPictureSeams = this.openPictureSeams.bind(this);
    this.handleAlertClose = this.handleAlertClose.bind(this);

    this.getMaxEnergyError = this.getMaxEnergyError.bind(this);
    this.getHeightWidthRatioError = this.getHeightWidthRatioError.bind(this);
    this.getHorzSeamsError = this.getHorzSeamsError.bind(this);
    this.getVertSeamsError = this.getVertSeamsError.bind(this);
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
        renderedImageSrc : response.finalImage,
        energyImageSrc : response.energyImage,
        seamImageSrc : response.seamImage,
        request : request
      }
    });
  }

  // parses video from request
  vidOnSuccess(request, response) {
    var that = this;

    var frameRate = 30;
    var encoder = new Whammy.Video(frameRate);
    response.forEach(function(dataurl) {
      var canvas = document.createElement('canvas');
      var ctx = canvas.getContext('2d');
      var image = document.createElement('img');
      image.src = dataurl;
      canvas.width = image.width;
      canvas.height = image.height;
      ctx.drawImage(image,0,0);
      encoder.add(canvas);
    });

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

  // packages response if image
  // onsuccessfunct is a function taking (request, response) argument
  // prevRequest (this function compares current request against previous request and returns cache if ===)
  // when all done, calls on Finish callback
  packageSendRequest(onSuccessFunct, serverURL, prevRequest, onFinishCallback) {
    if (this.state.origImage === null) {
      this.setState({errorAlert: ["Original image is not set"]});
      return;
    }

    var that = this;
    var canvas = document.createElement("canvas");
    canvas.width = this.state.origImageWidth;
    canvas.height = this.state.origImageHeight;
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

        // check error in height width ratio
        var heightWidthRatioErr = that.getHeightWidthRatioError();
        if (heightWidthRatioErr.length > 0) {
            that.setState({errorAlert: [heightWidthRatioErr]});
            return;
        }
      }
      // all settings
      else if (that.state.slideIndex == 2) {
        request.maxEnergy = that.state.maxEnergy;
        request.vertSeamsNum = that.state.vertSeamsNum;
        request.horzSeamsNum = that.state.horzSeamsNum;
        request.seamRemoval = that.state.seamRemoval;

        // check for errors in fields
        const maxEnergyError = that.getMaxEnergyError();
        const horzSeamsError = that.getHorzSeamsError();
        const vertSeamsError = that.getVertSeamsError();

        var errors = [];
        if (maxEnergyError.length > 0) errors.push(maxEnergyError);
        if (horzSeamsError.length > 0) errors.push(horzSeamsError);
        if (vertSeamsError.length > 0) errors.push(vertSeamsError);

        if (errors.length > 0) {
          that.setState({errorAlert: errors});
          return;
        }
      }

      // if the previous request doesn't equal this request, pull from server
      if (objectEquality(request, prevRequest)) {
        onFinishCallback();
      }
      else {
        that.serverRequest(serverURL, request,
          function(request, response) {
            onSuccessFunct(request, response);
            onFinishCallback();
          });
      }
    }
    img.src = this.state.origImage;
  }

  // show light box with img item
  lightbox(item) {
    $.fancybox({
      href : item,
      title : '<a style="color: #fff; text-decoration: none;" href="' + item + '" download>Click to Download</a>'
    });
  }

  openRenderedPicture() {
    var that = this;
    this.packageSendRequest(this.picOnSuccess, serverPicsURL, this.state.imageResponse.request,
      function() { that.lightbox(that.state.imageResponse.renderedImageSrc); });
  }

  openImageEnergy() {
    var that = this;
    this.packageSendRequest(this.picOnSuccess, serverPicsURL, this.state.imageResponse.request,
      function() { that.lightbox(that.state.imageResponse.energyImageSrc); });
  }

  openPictureSeams() {
    var that = this;
    this.packageSendRequest(this.picOnSuccess, serverPicsURL, this.state.imageResponse.request,
      function() { that.lightbox(that.state.imageResponse.seamImageSrc); });
  }

  openMakingOf() {
    var that = this;
    this.packageSendRequest(this.vidOnSuccess, serverVidURL, this.state.videoResponse.request,
      function() {
        var vid = that.state.videoResponse.makingOfSrc;
        $.fancybox(
          `<video controls>
               <source src="` + vid + `" />
               Your browser does not support HTML5 video.
             </video>`);
      });
  }

  handleAlertClose() {
    this.setState({errorAlert: []});
  }

  getHeightWidthRatioError() {
    return (this.state.heightWidthRatio <= 0) ?
        "The Height Width Ratio needs to be greater than 0" : "";
  }

  getMaxEnergyError() {
    return this.dimensionError(this.state.maxEnergy, 0, 1,
      "The maximum energy has to be between 0 and 1");
  }

  getHorzSeamsError() {
    console.debug("horizontal seams and and height", this.state.horzSeamsNum, this.state.origImageHeight);
    return this.dimensionError(this.state.horzSeamsNum, 0, this.state.origImageHeight - 1,
      "Cannot remove negative seams and must remove less seams than the original image height");
  }

  getVertSeamsError() {
    console.debug("vert seams and and width", this.state.vertSeamsNum, this.state.origImageWidth);
    return this.dimensionError(this.state.vertSeamsNum, 0, this.state.origImageWidth - 1,
      "Cannot remove negative seams and must remove less seams than the original image width");
  }

  render() {
    const that = this;

    const heightWidthRatioError = this.getHeightWidthRatioError();
    const maxEnergyError = this.getMaxEnergyError();
    const horzSeamsError = this.getHorzSeamsError();
    const vertSeamsError = this.getVertSeamsError();

      const OKButton = [
        <FlatButton
          label="OK"
          primary={true}
          onTouchTap={this.handleAlertClose}
        />
      ];

    const heightWidthRatioUpdate = function(e) { that.setState({heightWidthRatio : parseFloat(e.target.value) }); }
    const maxEnergyUpdate = function(e) { that.setState({maxEnergy : parseFloat(e.target.value) }); }
    const horzSeamsFieldUpdate = function(e) { that.setState({horzSeamsNum : parseInt(e.target.value, 10) }); }
    const vertSeamsFieldUpdate = function(e) { that.setState({vertSeamsNum : parseInt(e.target.value, 10) }); }
    const seamRemovalUpdate = function(e,v) { that.setState({seamRemoval : !that.state.seamRemoval }); }

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

    var errorText = this.state.errorAlert.map(function(item) {
      return (<p>{item}</p>);
    });

    return (
      <MuiThemeProvider muiTheme={muiTheme}>
      <div>
        <Dialog
          actions={OKButton}
          modal={false}
          open={this.state.errorAlert.length > 0}
          onRequestClose={this.handleAlertClose}
          title="There was an Error"
        >
          {errorText}
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
              <Checkbox
                label="Remove seams"
                checked={this.state.seamRemoval}
                onCheck={seamRemovalUpdate}
              />
              <TextField
                style={styles.textfield}
                floatingLabelText="Ratio of height to width"
                type="number"
                onChange={heightWidthRatioUpdate}
                errorText={heightWidthRatioError}
              />
            </div>
            <div style={styles.swipeableDiv}>
              All options for the seam carving process.
              <Checkbox
                label="Remove seams"
                checked={this.state.seamRemoval}
                onCheck={seamRemovalUpdate}
              />
              <div>
                <TextField
                  style={styles.textfield}
                  floatingLabelText="Vertical seams to remove"
                  type="number"
                  onChange={vertSeamsFieldUpdate}
                  errorText={vertSeamsError}
                />
                <TextField
                  style={styles.textfield}
                  floatingLabelText="Horizontal seams to remove"
                  type="number"
                  onChange={horzSeamsFieldUpdate}
                  errorText={horzSeamsError}
                />
              </div>
              <TextField
                style={styles.textfield}
                floatingLabelText="Maximum pixel energy"
                type="number"
                onChange={maxEnergyUpdate}
                errorText={maxEnergyError}
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
            label="View Seams"
            onTouchTap={this.openPictureSeams}
            icon={<ImageBlurOff />}
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
