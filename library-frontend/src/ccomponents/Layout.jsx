import Navbar from './Navbar'
import Sidebar from './Sidebar'
import './Components.css'
const Layout = ({children}) => {

  return (
    <>
 
       
      <Navbar />

      <div className="layout">
        <Sidebar />

        <div className="content">
          {children}
        </div>
      </div>
    </>
    
  )
}

export default Layout
