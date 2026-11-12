import { createRoot } from 'react-dom/client'
import 'bootstrap/dist/css/bootstrap.min.css';
import { BrowserRouter } from "react-router-dom";
import App from './App'
import 'react-toastify/dist/ReactToastify.css';
import { ToastContainer } from 'react-toastify';
import "./ccomponents/service/AxiosInterceptor";

createRoot(document.getElementById('root')).render(
    <BrowserRouter>
    <App />
<ToastContainer
        position="top-right"
        autoClose={3000}
        hideProgressBar={false}
    />
  </BrowserRouter>
)
