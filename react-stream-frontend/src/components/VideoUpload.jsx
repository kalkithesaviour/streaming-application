import { useState } from "react";
import axios from "axios";
import videoLogo from "../assets/video-posting.png";
import toast from "react-hot-toast";
import {
  Button,
  Card,
  Label,
  Textarea,
  TextInput,
  Progress,
  Alert,
} from "flowbite-react";

function VideoUpload() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [meta, setMeta] = useState({
    title: "",
    description: "",
  });
  const [progress, setProgress] = useState(0);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState("");
  const [alertType, setAlertType] = useState("");

  function handleFileChange(event) {
    setSelectedFile(event.target.files[0]);
  }

  function formFieldChange(event) {
    setMeta({
      ...meta,
      [event.target.name]: event.target.value,
    });
  }

  function handleForm(formEvent) {
    formEvent.preventDefault();
    if (!selectedFile) {
      alert("Select file!!");
      return;
    }
    saveVideoToServer(selectedFile, meta);
  }

  function resetForm() {
    setMeta({
      title: "",
      description: "",
    });
    setSelectedFile(null);
    setUploading(false);
  }

  async function saveVideoToServer(video, videoMetadata) {
    setUploading(true);

    try {
      let formData = new FormData();
      formData.append("title", videoMetadata.title);
      formData.append("description", videoMetadata.description);
      formData.append("file", selectedFile);

      await axios.post(`http://localhost:8080/api/v1/videos`, formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
        onUploadProgress: (progressEvent) => {
          const progress = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          setProgress(progress);
        },
      });
      setProgress(0);

      setMessage("File uploaded successfully!!");
      setAlertType("success");
      setUploading(false);

      toast.success("File uploaded successfully!!");

      resetForm();
    } catch (error) {
      console.log(error);
      setMessage("Error in uploading file!!");
      setAlertType("error");
      setUploading(false);

      toast.error("File not uploaded!!");
    }
  }

  return (
    <div className="text-white">
      <Card className="flex flex-col">
        <h1>Upload Videos</h1>
        <div>
          <form noValidate onSubmit={handleForm} className="space-y-6">
            <div>
              <div className="mb-2 block">
                <Label htmlFor="video-title" value="Video Title" />
              </div>
              <TextInput
                id="video-title"
                value={meta.title}
                onChange={formFieldChange}
                name="title"
                placeholder="Enter title"
              />
            </div>

            <div className="max-w-md">
              <div className="mb-2 block">
                <Label htmlFor="video-description" value="Video Description" />
              </div>
              <Textarea
                id="video-description"
                value={meta.description}
                onChange={formFieldChange}
                name="description"
                placeholder="Write video description..."
                required
                rows={4}
              />
            </div>

            <div className="flex items-center space-x-5 justify-center">
              <div className="shrink-0">
                <img
                  className="h-16 w-16 object-cover"
                  src={videoLogo}
                  alt="Choose video file"
                />
              </div>
              <label className="block">
                <span className="sr-only">Choose video file</span>
                <input
                  name="file"
                  onChange={handleFileChange}
                  type="file"
                  className="block w-full text-sm text-slate-500
              file:mr-4 file:py-2 file:px-4
              file:rounded-full file:border-0
              file:text-sm file:font-semibold
              file:bg-violet-50 file:text-violet-700
              hover:file:bg-violet-100"
                />
              </label>
            </div>

            <div>
              {uploading && (
                <Progress
                  progress={progress}
                  color="pink"
                  textLabel="Uploading"
                  size="xl"
                  labelProgress
                  labelText
                />
              )}
            </div>

            <div>
              {message && (
                <Alert
                  color={alertType === "success" ? "success" : "failure"}
                  rounded
                  withBorderAccent
                  onDismiss={() => {
                    setMessage("");
                  }}
                >
                  <span className="font-medium">
                    {alertType === "success" ? "Success!" : "Error!"}{" "}
                  </span>
                  {message}
                </Alert>
              )}
            </div>

            <div className="flex justify-center">
              <Button disabled={uploading} type="submit">
                Submit
              </Button>
            </div>
          </form>
        </div>
      </Card>
    </div>
  );
}

export default VideoUpload;
